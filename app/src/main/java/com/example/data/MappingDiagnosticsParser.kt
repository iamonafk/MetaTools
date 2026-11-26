package com.example.data

import com.example.model.DiagnosticIssue
import com.example.model.DiagnosticIssueType
import com.example.model.MappingParseDiagnostics
import com.example.model.SourcePreviewInfo
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import com.squareup.moshi.JsonReader
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.InputStream

object MappingDiagnosticsParser {

    fun parseJsonString(rawJson: String, sourceName: String): MappingParseDiagnostics {
        val bytes = rawJson.toByteArray(Charsets.UTF_8)
        return parseStream(ByteArrayInputStream(bytes), sourceName, knownByteSize = bytes.size.toLong())
    }

    fun parseStream(
        inputStream: InputStream,
        sourceName: String,
        knownByteSize: Long = -1L
    ): MappingParseDiagnostics {
        val validFlags = ArrayList<UniversalFlag>(6000)
        val previewEntries = ArrayList<String>(5)
        val duplicateUniversalEntries = ArrayList<DiagnosticIssue>()
        val duplicateSubFlagEntries = ArrayList<DiagnosticIssue>()
        val malformedEntries = ArrayList<DiagnosticIssue>()
        val missingNameEntries = ArrayList<DiagnosticIssue>()
        val unknownCategoryEntries = ArrayList<DiagnosticIssue>()
        val emptySubFlagEntries = ArrayList<DiagnosticIssue>()
        val subFlagConsistencyIssues = ArrayList<DiagnosticIssue>()

        val seenUniversalNames = HashSet<String>(6000)
        val seenUniversalCodes = HashSet<String>(6000)

        var rawEntryCount = 0
        var totalSubFlagsCount = 0
        var countedBytes = 0L

        return try {
            val allBytes = inputStream.readBytes()
            countedBytes = if (knownByteSize >= 0L) knownByteSize else allBytes.size.toLong()
            val buffer = Buffer().write(allBytes)
            parseBufferedSource(
                bufferedSource = buffer,
                sourceName = sourceName,
                byteSize = countedBytes,
                validFlags = validFlags,
                previewEntries = previewEntries,
                duplicateUniversalEntries = duplicateUniversalEntries,
                duplicateSubFlagEntries = duplicateSubFlagEntries,
                malformedEntries = malformedEntries,
                missingNameEntries = missingNameEntries,
                unknownCategoryEntries = unknownCategoryEntries,
                emptySubFlagEntries = emptySubFlagEntries,
                subFlagConsistencyIssues = subFlagConsistencyIssues,
                seenUniversalNames = seenUniversalNames,
                seenUniversalCodes = seenUniversalCodes
            ) { count, subCount ->
                rawEntryCount = count
                totalSubFlagsCount = subCount
            }
        } catch (e: Exception) {
            MappingParseDiagnostics(
                sourceName = sourceName,
                validFlags = emptyList(),
                rawEntryCount = rawEntryCount,
                validEntryCount = 0,
                totalSubFlagsCount = 0,
                byteSize = countedBytes,
                previewEntries = previewEntries,
                duplicateUniversalEntries = duplicateUniversalEntries,
                duplicateSubFlagEntries = duplicateSubFlagEntries,
                malformedEntries = malformedEntries,
                missingNameEntries = missingNameEntries,
                unknownCategoryEntries = unknownCategoryEntries,
                isValid = false,
                validationError = e.localizedMessage ?: "Invalid JSON mapping format",
                emptySubFlagEntries = emptySubFlagEntries,
                subFlagConsistencyIssues = subFlagConsistencyIssues
            )
        }
    }

    private inline fun parseBufferedSource(
        bufferedSource: BufferedSource,
        sourceName: String,
        byteSize: Long,
        validFlags: ArrayList<UniversalFlag>,
        previewEntries: ArrayList<String>,
        duplicateUniversalEntries: ArrayList<DiagnosticIssue>,
        duplicateSubFlagEntries: ArrayList<DiagnosticIssue>,
        malformedEntries: ArrayList<DiagnosticIssue>,
        missingNameEntries: ArrayList<DiagnosticIssue>,
        unknownCategoryEntries: ArrayList<DiagnosticIssue>,
        emptySubFlagEntries: ArrayList<DiagnosticIssue>,
        subFlagConsistencyIssues: ArrayList<DiagnosticIssue>,
        seenUniversalNames: HashSet<String>,
        seenUniversalCodes: HashSet<String>,
        onCountsReady: (Int, Int) -> Unit
    ): MappingParseDiagnostics {
        var rawEntryCount = 0
        var totalSubFlagsCount = 0

        val jsonReader = JsonReader.of(bufferedSource)
        jsonReader.isLenient = true

        if (jsonReader.peek() != JsonReader.Token.BEGIN_ARRAY) {
            return MappingParseDiagnostics(
                sourceName = sourceName,
                validFlags = emptyList(),
                rawEntryCount = 0,
                validEntryCount = 0,
                totalSubFlagsCount = 0,
                byteSize = byteSize,
                previewEntries = emptyList(),
                duplicateUniversalEntries = emptyList(),
                duplicateSubFlagEntries = emptyList(),
                malformedEntries = emptyList(),
                missingNameEntries = emptyList(),
                unknownCategoryEntries = emptyList(),
                isValid = false,
                validationError = "Expected a JSON array of mapping strings"
            )
        }

        jsonReader.beginArray()
        while (jsonReader.hasNext()) {
            val idx = rawEntryCount++
            val token = jsonReader.peek()
            if (token != JsonReader.Token.STRING) {
                jsonReader.skipValue()
                malformedEntries.add(
                    DiagnosticIssue(
                        type = DiagnosticIssueType.MALFORMED_ENTRY,
                        entryIndex = idx,
                        rawEntry = "<non-string token: $token>",
                        reason = "Expected string entry inside mapping JSON array"
                    )
                )
                continue
            }

            val raw = jsonReader.nextString()
            val trimmed = raw.trim()
            if (previewEntries.size < 5 && trimmed.isNotEmpty()) {
                previewEntries.add(trimmed)
            }

            if (trimmed.isEmpty()) {
                malformedEntries.add(
                    DiagnosticIssue(
                        type = DiagnosticIssueType.MALFORMED_ENTRY,
                        entryIndex = idx,
                        rawEntry = raw,
                        reason = "Blank entry string"
                    )
                )
                continue
            }

            val parts = trimmed.split(':')
            if (parts.size < 2) {
                malformedEntries.add(
                    DiagnosticIssue(
                        type = DiagnosticIssueType.MALFORMED_ENTRY,
                        entryIndex = idx,
                        rawEntry = trimmed,
                        reason = "Missing ':' separator between universal code and universal name"
                    )
                )
                continue
            }

            val universalCode = parts[0].trim()
            val universalName = parts[1].trim()

            if (universalCode.isEmpty() || universalName.isEmpty()) {
                missingNameEntries.add(
                    DiagnosticIssue(
                        type = DiagnosticIssueType.MISSING_NAME,
                        entryIndex = idx,
                        rawEntry = trimmed,
                        flagName = universalName,
                        flagCode = universalCode,
                        reason = if (universalName.isEmpty()) {
                            "Missing universal flag name for code '$universalCode'"
                        } else {
                            "Missing universal flag code for flag '$universalName'"
                        }
                    )
                )
                continue
            }

            val category = MetaConfigParser.detectCategory(universalName)
            if (category == "Other") {
                unknownCategoryEntries.add(
                    DiagnosticIssue(
                        type = DiagnosticIssueType.UNKNOWN_CATEGORY,
                        entryIndex = idx,
                        rawEntry = trimmed,
                        flagName = universalName,
                        flagCode = universalCode,
                        reason = "No category rule matched; classified under 'Other'"
                    )
                )
            }

            // Duplicate universal detection (name / code)
            if (!seenUniversalNames.add(universalName)) {
                duplicateUniversalEntries.add(
                    DiagnosticIssue(
                        type = DiagnosticIssueType.DUPLICATE_UNIVERSAL_NAME,
                        entryIndex = idx,
                        rawEntry = trimmed,
                        flagName = universalName,
                        flagCode = universalCode,
                        reason = "Duplicate universal flag name '$universalName'"
                    )
                )
            }
            if (!seenUniversalCodes.add(universalCode)) {
                duplicateUniversalEntries.add(
                    DiagnosticIssue(
                        type = DiagnosticIssueType.DUPLICATE_UNIVERSAL_CODE,
                        entryIndex = idx,
                        rawEntry = trimmed,
                        flagName = universalName,
                        flagCode = universalCode,
                        reason = "Duplicate universal flag code '$universalCode' (also on '$universalName')"
                    )
                )
            }

            // Parse subflags cleanly without confusing universal code & subflag code
            val subFlags = ArrayList<SubFlag>()
            val seenSubNames = HashMap<String, String>()
            val seenSubCodes = HashMap<String, String>()
            var hasDanglingSubPart = false

            var i = 2
            while (i < parts.size) {
                val subCode = parts[i].trim()
                if (i + 1 >= parts.size) {
                    if (subCode.isNotEmpty()) {
                        hasDanglingSubPart = true
                        missingNameEntries.add(
                            DiagnosticIssue(
                                type = DiagnosticIssueType.MISSING_NAME,
                                entryIndex = idx,
                                rawEntry = trimmed,
                                flagName = universalName,
                                flagCode = universalCode,
                                reason = "Subflag code '$subCode' is missing its subflag name"
                            )
                        )
                    } else {
                        emptySubFlagEntries.add(
                            DiagnosticIssue(
                                type = DiagnosticIssueType.EMPTY_SUBFLAG,
                                entryIndex = idx,
                                rawEntry = trimmed,
                                flagName = universalName,
                                flagCode = universalCode,
                                reason = "Trailing empty subflag delimiter in '$universalName'"
                            )
                        )
                    }
                    break
                }

                val subName = parts[i + 1].trim()
                if (subCode.isEmpty() && subName.isEmpty()) {
                    emptySubFlagEntries.add(
                        DiagnosticIssue(
                            type = DiagnosticIssueType.EMPTY_SUBFLAG,
                            entryIndex = idx,
                            rawEntry = trimmed,
                            flagName = universalName,
                            flagCode = universalCode,
                            reason = "Empty subflag segment inside '$universalName'"
                        )
                    )
                } else if (subCode.isEmpty() || subName.isEmpty()) {
                    missingNameEntries.add(
                        DiagnosticIssue(
                            type = DiagnosticIssueType.MISSING_NAME,
                            entryIndex = idx,
                            rawEntry = trimmed,
                            flagName = universalName,
                            flagCode = universalCode,
                            reason = "Incomplete subflag pair ('$subCode:$subName')"
                        )
                    )
                } else {
                    val prevCodeForName = seenSubNames.putIfAbsent(subName, subCode)
                    val prevNameForCode = seenSubCodes.putIfAbsent(subCode, subName)
                    if (prevCodeForName != null || prevNameForCode != null) {
                        duplicateSubFlagEntries.add(
                            DiagnosticIssue(
                                type = DiagnosticIssueType.DUPLICATE_SUBFLAG,
                                entryIndex = idx,
                                rawEntry = trimmed,
                                flagName = universalName,
                                flagCode = universalCode,
                                reason = "Duplicate subflag '$subName' ($subCode) inside '$universalName'"
                            )
                        )
                        if (prevNameForCode != null && prevNameForCode != subName) {
                            subFlagConsistencyIssues.add(
                                DiagnosticIssue(
                                    type = DiagnosticIssueType.SUBFLAG_CODE_CONFLICT,
                                    entryIndex = idx,
                                    rawEntry = trimmed,
                                    flagName = universalName,
                                    flagCode = universalCode,
                                    reason = "Subflag code '$subCode' maps to both '$prevNameForCode' and '$subName' in '$universalName'"
                                )
                            )
                        }
                    }
                    subFlags.add(
                        SubFlag(
                            code = subCode,
                            name = subName,
                            parentFlagCode = universalCode,
                            parentFlagName = universalName,
                            category = category
                        )
                    )
                }
                i += 2
            }

            if (hasDanglingSubPart && subFlags.isEmpty()) {
                malformedEntries.add(
                    DiagnosticIssue(
                        type = DiagnosticIssueType.MALFORMED_ENTRY,
                        entryIndex = idx,
                        rawEntry = trimmed,
                        flagName = universalName,
                        flagCode = universalCode,
                        reason = "Trailing odd subflag segment in '$universalName'"
                    )
                )
            }

            totalSubFlagsCount += subFlags.size
            validFlags.add(
                UniversalFlag(
                    code = universalCode,
                    name = universalName,
                    subFlags = subFlags,
                    category = category,
                    originalEntry = trimmed
                )
            )
        }
        jsonReader.endArray()
        onCountsReady(rawEntryCount, totalSubFlagsCount)

        val isValid = validFlags.isNotEmpty()
        val validationError = when {
            rawEntryCount == 0 -> "JSON array is empty"
            validFlags.isEmpty() -> "No valid MetaConfig mapping entries found"
            else -> null
        }

        return MappingParseDiagnostics(
            sourceName = sourceName,
            validFlags = validFlags,
            rawEntryCount = rawEntryCount,
            validEntryCount = validFlags.size,
            totalSubFlagsCount = totalSubFlagsCount,
            byteSize = byteSize,
            previewEntries = previewEntries,
            duplicateUniversalEntries = duplicateUniversalEntries,
            duplicateSubFlagEntries = duplicateSubFlagEntries,
            malformedEntries = malformedEntries,
            missingNameEntries = missingNameEntries,
            unknownCategoryEntries = unknownCategoryEntries,
            isValid = isValid,
            validationError = validationError,
            emptySubFlagEntries = emptySubFlagEntries,
            subFlagConsistencyIssues = subFlagConsistencyIssues
        )
    }

    fun toSourcePreview(diagnostics: MappingParseDiagnostics): SourcePreviewInfo {
        val statusMsg = when {
            !diagnostics.isValid -> diagnostics.validationError ?: "Invalid mapping file"
            diagnostics.invalidCount > 0 || diagnostics.duplicateCount > 0 ->
                "Valid (${diagnostics.invalidCount} invalid, ${diagnostics.duplicateCount} duplicates)"
            else -> "Valid MetaConfig Mapping"
        }
        return SourcePreviewInfo(
            sourceName = diagnostics.sourceName,
            isValid = diagnostics.isValid,
            validationMessage = statusMsg,
            universalFlagCount = diagnostics.validEntryCount,
            subFlagCount = diagnostics.totalSubFlagsCount,
            byteSize = diagnostics.byteSize,
            duplicateCount = diagnostics.duplicateCount,
            invalidCount = diagnostics.invalidCount,
            previewEntries = diagnostics.previewEntries
        )
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
