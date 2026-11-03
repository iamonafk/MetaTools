package com.example.data

import com.example.model.AnalysisDashboardStats
import com.example.model.AnalyzedSubFlag
import com.example.model.AnalyzedUniversalFlag
import com.example.model.BackupFileType
import com.example.model.CategoryAnalysisSummary
import com.example.model.IBackupInfo
import com.example.model.ModifiedFlagItem
import com.example.model.ParsedBackupState
import com.example.model.UniversalFlag
import com.example.model.UnmappedFlagItem
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

object FlagAnalysisEngine {

    /**
     * Parses an imported iBackup or normal JSON backup string against the active mapping database.
     * Extracts all universal flags, subflags, and their current enabled states.
     * Retains original raw JSON structures for lossless export.
     */
    fun parseBackup(
        content: String,
        fileName: String,
        mappingFlags: List<UniversalFlag>
    ): Result<ParsedBackupState> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Imported file is empty."))
        }

        return try {
            val rootJson = JSONObject(trimmed)

            // Detect whether this is an iBackup envelope or a standalone JSON backup
            val isIbackup = rootJson.has("backup") && rootJson.opt("backup") is JSONObject
            val fileType = if (isIbackup) BackupFileType.IBACKUP else BackupFileType.JSON

            val backupObj: JSONObject = if (isIbackup) {
                rootJson.getJSONObject("backup")
            } else {
                rootJson
            }

            val manifestVersion = if (isIbackup) rootJson.optInt("manifest_version", 2) else null
            val infoObj = if (isIbackup) rootJson.optJSONObject("info") else null
            val info = infoObj?.let {
                IBackupInfo(
                    id = it.optLong("id", System.currentTimeMillis()),
                    releaseId = it.optString("release_id", ""),
                    name = it.optString("name", fileName.substringBeforeLast('.')),
                    author = it.optString("author", "MetaTools User"),
                    authorId = it.optLong("author_id", 0L),
                    version = it.optString("version", "1"),
                    releaseDate = it.optString("release_date", ""),
                    changelog = it.optString("changelog", "")
                )
            }

            // Index mapping flags for quick lookup
            val universalMap = HashMap<String, UniversalFlag>(mappingFlags.size)
            for (f in mappingFlags) {
                universalMap[f.code.trim()] = f
            }

            val analyzedFlags = ArrayList<AnalyzedUniversalFlag>()

            val keys = backupObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key == "qe_overrides") {
                    continue
                }

                val value = backupObj.opt(key)
                if (value !is JSONArray) {
                    continue
                }

                val universalCode = key.trimEnd(':').trim()
                val mappedUniversal = universalMap[universalCode]

                val isUniversalUnknown = mappedUniversal == null
                val flagName = mappedUniversal?.name ?: "Unknown Flag ($universalCode)"
                val category = when {
                    mappedUniversal != null && mappedUniversal.category.isNotBlank() -> mappedUniversal.category
                    mappedUniversal != null -> MetaConfigParser.detectCategory(mappedUniversal.name)
                    else -> "Other"
                }

                val subFlagMap = mappedUniversal?.subFlags?.associateBy { it.code.trim() } ?: emptyMap()

                val subFlagsList = ArrayList<AnalyzedSubFlag>(value.length())
                for (i in 0 until value.length()) {
                    val entryStr = value.optString(i, "").trim()
                    if (entryStr.isEmpty()) continue

                    val (subCode, isEnabled) = parseSubFlagEntry(entryStr)
                    val mappedSub = subFlagMap[subCode]
                    val isSubUnknown = mappedSub == null
                    val subName = mappedSub?.name ?: "Unknown Sub-Flag ($subCode)"

                    subFlagsList.add(
                        AnalyzedSubFlag(
                            subCode = subCode,
                            name = subName,
                            isUnknown = isSubUnknown,
                            isEnabled = isEnabled,
                            originalEnabled = isEnabled,
                            rawEntryString = entryStr
                        )
                    )
                }

                analyzedFlags.add(
                    AnalyzedUniversalFlag(
                        code = universalCode,
                        rawKey = key,
                        name = flagName,
                        category = category,
                        isUnknown = isUniversalUnknown,
                        subFlags = subFlagsList
                    )
                )
            }

            // Sort: Known flags first, then alphabetically by name
            analyzedFlags.sortWith(
                compareBy<AnalyzedUniversalFlag> { it.isUnknown }
                    .thenBy { it.category }
                    .thenBy { it.name }
            )

            val parsedState = ParsedBackupState(
                fileName = fileName,
                fileType = fileType,
                rawRootJson = rootJson,
                rawBackupJson = backupObj,
                info = info,
                manifestVersion = manifestVersion,
                flags = analyzedFlags
            )

            Result.success(parsedState)
        } catch (e: JSONException) {
            Result.failure(IllegalArgumentException("Invalid JSON format: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts the subflag code and boolean value from strings like:
     * "23: : false", "0: : true", "4: : true", "12:true", "5: : 1"
     */
    fun parseSubFlagEntry(entry: String): Pair<String, Boolean> {
        val trimmed = entry.trim()
        val subCode = trimmed.substringBefore(':').trim()

        val lower = trimmed.lowercase(Locale.ROOT)
        val isEnabled = when {
            lower.endsWith("true") -> true
            lower.endsWith(": 1") || lower.endsWith(":1") -> true
            lower.endsWith("false") -> false
            lower.endsWith(": 0") || lower.endsWith(":0") -> false
            else -> false
        }

        return Pair(subCode, isEnabled)
    }

    /**
     * Generates a new subflag entry string preserving original colon spacing and layout.
     */
    fun formatSubFlagEntry(originalEntry: String, newEnabled: Boolean): String {
        val boolRegex = Regex("""(?i)\b(true|false)\b""")
        return if (boolRegex.containsMatchIn(originalEntry)) {
            originalEntry.replace(boolRegex, newEnabled.toString())
        } else {
            val subCode = originalEntry.substringBefore(':').trim()
            "$subCode: : $newEnabled"
        }
    }

    /**
     * Updates an individual subflag's enabled state.
     */
    fun updateSubFlagState(
        flags: List<AnalyzedUniversalFlag>,
        universalCode: String,
        subCode: String,
        newEnabled: Boolean
    ): List<AnalyzedUniversalFlag> {
        return flags.map { uFlag ->
            if (uFlag.code == universalCode) {
                val updatedSubFlags = uFlag.subFlags.map { sFlag ->
                    if (sFlag.subCode == subCode) {
                        sFlag.copy(isEnabled = newEnabled)
                    } else {
                        sFlag
                    }
                }
                uFlag.copy(subFlags = updatedSubFlags)
            } else {
                uFlag
            }
        }
    }

    /**
     * Toggles an individual subflag's enabled state.
     */
    fun toggleSubFlagState(
        flags: List<AnalyzedUniversalFlag>,
        universalCode: String,
        subCode: String
    ): List<AnalyzedUniversalFlag> {
        return flags.map { uFlag ->
            if (uFlag.code == universalCode) {
                val updatedSubFlags = uFlag.subFlags.map { sFlag ->
                    if (sFlag.subCode == subCode) {
                        sFlag.copy(isEnabled = !sFlag.isEnabled)
                    } else {
                        sFlag
                    }
                }
                uFlag.copy(subFlags = updatedSubFlags)
            } else {
                uFlag
            }
        }
    }

    /**
     * Sets enabled state for all flags in a specific category.
     */
    fun setCategoryFlagsState(
        flags: List<AnalyzedUniversalFlag>,
        category: String,
        newEnabled: Boolean
    ): List<AnalyzedUniversalFlag> {
        return flags.map { uFlag ->
            if (uFlag.category == category) {
                val updatedSubFlags = uFlag.subFlags.map { sFlag ->
                    sFlag.copy(isEnabled = newEnabled)
                }
                uFlag.copy(subFlags = updatedSubFlags)
            } else {
                uFlag
            }
        }
    }

    /**
     * Sets enabled state for all flags across the entire backup.
     */
    fun setAllFlagsState(
        flags: List<AnalyzedUniversalFlag>,
        newEnabled: Boolean
    ): List<AnalyzedUniversalFlag> {
        return flags.map { uFlag ->
            val updatedSubFlags = uFlag.subFlags.map { sFlag ->
                sFlag.copy(isEnabled = newEnabled)
            }
            uFlag.copy(subFlags = updatedSubFlags)
        }
    }

    /**
     * Resets a single subflag to its original state when imported.
     */
    fun resetSubFlagToOriginal(
        flags: List<AnalyzedUniversalFlag>,
        universalCode: String,
        subCode: String
    ): List<AnalyzedUniversalFlag> {
        return flags.map { uFlag ->
            if (uFlag.code == universalCode) {
                val updatedSubFlags = uFlag.subFlags.map { sFlag ->
                    if (sFlag.subCode == subCode) {
                        sFlag.copy(isEnabled = sFlag.originalEnabled)
                    } else {
                        sFlag
                    }
                }
                uFlag.copy(subFlags = updatedSubFlags)
            } else {
                uFlag
            }
        }
    }

    /**
     * Resets all flags across the entire backup to their original imported states.
     */
    fun resetAllFlagsToOriginal(
        flags: List<AnalyzedUniversalFlag>
    ): List<AnalyzedUniversalFlag> {
        return flags.map { uFlag ->
            val updatedSubFlags = uFlag.subFlags.map { sFlag ->
                sFlag.copy(isEnabled = sFlag.originalEnabled)
            }
            uFlag.copy(subFlags = updatedSubFlags)
        }
    }

    /**
     * Collects all modified flags into a clean list for the Change Tracking / Compare view.
     */
    fun getModifiedFlags(flags: List<AnalyzedUniversalFlag>): List<ModifiedFlagItem> {
        val list = ArrayList<ModifiedFlagItem>()
        for (uFlag in flags) {
            for (sFlag in uFlag.subFlags) {
                if (sFlag.isModified) {
                    list.add(
                        ModifiedFlagItem(
                            universalCode = uFlag.code,
                            universalName = uFlag.name,
                            subCode = sFlag.subCode,
                            subName = sFlag.name,
                            category = uFlag.category,
                            originalState = sFlag.originalEnabled,
                            currentState = sFlag.isEnabled
                        )
                    )
                }
            }
        }
        return list
    }

    /**
     * Finds all unmapped flags and subflags.
     */
    fun getUnmappedFlags(flags: List<AnalyzedUniversalFlag>): List<UnmappedFlagItem> {
        val list = ArrayList<UnmappedFlagItem>()
        for (uFlag in flags) {
            val unmappedSubCodes = uFlag.subFlags.filter { it.isUnknown }.map { it.subCode }
            if (uFlag.isUnknown || unmappedSubCodes.isNotEmpty()) {
                list.add(
                    UnmappedFlagItem(
                        universalCode = uFlag.code,
                        rawKey = uFlag.rawKey,
                        unmappedSubCodes = unmappedSubCodes,
                        isUniversalUnmapped = uFlag.isUnknown
                    )
                )
            }
        }
        return list
    }

    /**
     * Generates a category-wise summary report.
     */
    fun getCategorySummaries(flags: List<AnalyzedUniversalFlag>): List<CategoryAnalysisSummary> {
        return flags.groupBy { it.category }
            .map { (cat, catFlags) ->
                val totalSub = catFlags.sumOf { it.totalSubFlags }
                val enabledSub = catFlags.sumOf { it.enabledSubFlags }
                val disabledSub = catFlags.sumOf { it.disabledSubFlags }
                val modifiedSub = catFlags.sumOf { f -> f.subFlags.count { it.isModified } }
                CategoryAnalysisSummary(
                    category = cat,
                    universalCount = catFlags.size,
                    subFlagCount = totalSub,
                    enabledCount = enabledSub,
                    disabledCount = disabledSub,
                    modifiedCount = modifiedSub
                )
            }
            .sortedWith(
                compareByDescending<CategoryAnalysisSummary> { it.modifiedCount }
                    .thenByDescending { it.universalCount }
            )
    }

    /**
     * Generates the real-time dashboard statistics.
     */
    fun calculateDashboardStats(flags: List<AnalyzedUniversalFlag>): AnalysisDashboardStats {
        val totalUniversal = flags.size
        val totalSub = flags.sumOf { it.totalSubFlags }
        val enabledSub = flags.sumOf { it.enabledSubFlags }
        val disabledSub = flags.sumOf { it.disabledSubFlags }
        val unknownCount = flags.count { it.isUnknown } + flags.sumOf { f -> f.subFlags.count { it.isUnknown } }
        val modifiedCount = flags.sumOf { f -> f.subFlags.count { it.isModified } }
        val percentage = if (totalSub == 0) 0f else (enabledSub.toFloat() / totalSub.toFloat()) * 100f

        return AnalysisDashboardStats(
            totalUniversalFlags = totalUniversal,
            totalSubFlags = totalSub,
            enabledSubFlags = enabledSub,
            disabledSubFlags = disabledSub,
            unknownCount = unknownCount,
            modifiedCount = modifiedCount,
            enabledPercentage = percentage
        )
    }

    /**
     * Validates the parsed backup before export.
     * Returns a list of error strings, or empty list if valid.
     */
    fun validateBackup(state: ParsedBackupState): List<String> {
        val errors = ArrayList<String>()

        if (state.flags.isEmpty()) {
            errors.add("Backup does not contain any universal flag entries.")
        }

        if (state.fileType == BackupFileType.IBACKUP) {
            if (state.manifestVersion == null || state.manifestVersion < 1) {
                errors.add("iBackup manifest_version is missing or invalid.")
            }
        }

        return errors
    }

    /**
     * Exports the updated backup preserving the original structure, metadata,
     * untouched flags, and qe_overrides.
     */
    fun generateExportContent(state: ParsedBackupState): Result<String> {
        return try {
            // Build a clone of the original backup JSON
            val updatedBackupJson = JSONObject(state.rawBackupJson.toString())

            // Map each analyzed flag back into the JSON array
            for (uFlag in state.flags) {
                val array = JSONArray()
                for (sFlag in uFlag.subFlags) {
                    val updatedEntry = formatSubFlagEntry(sFlag.rawEntryString, sFlag.isEnabled)
                    array.put(updatedEntry)
                }
                updatedBackupJson.put(uFlag.rawKey, array)
            }

            val finalOutput = if (state.fileType == BackupFileType.IBACKUP) {
                val updatedRoot = JSONObject(state.rawRootJson.toString())
                updatedRoot.put("backup", updatedBackupJson)
                updatedRoot.toString(2)
            } else {
                updatedBackupJson.toString(2)
            }

            Result.success(finalOutput)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Formats suggested filename for export.
     */
    fun getSuggestedExportFileName(state: ParsedBackupState): String {
        val baseName = state.fileName.substringBeforeLast('.')
            .replace(Regex("""[^a-zA-Z0-9_\- ]"""), "_")
            .trim()
            .ifEmpty { "modified_backup" }

        val ext = if (state.fileType == BackupFileType.IBACKUP) "ibackup" else "json"
        return "${baseName}_modified.$ext"
    }
}
