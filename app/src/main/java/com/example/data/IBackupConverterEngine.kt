package com.example.data

import com.example.model.ConversionMode
import com.example.model.ConversionPreview
import com.example.model.IBackupInfo
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.random.Random

object IBackupConverterEngine {

    /**
     * Converts an Instagram iBackup file (or raw string) into a standalone JSON backup file.
     * Extracts ONLY the contents of the "backup" field, preserving all universal flag keys,
     * sub-flag entries, qe_overrides, and unknown parameters without alteration.
     */
    fun convertIbackupToJson(
        inputContent: String,
        sourceFileName: String? = null
    ): Result<Triple<String, ConversionPreview, String>> {
        val trimmed = inputContent.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Input file content is empty."))
        }

        return try {
            val root = JSONObject(trimmed)
            val infoObj = root.optJSONObject("info")
            val manifestVersion = root.optInt("manifest_version", -1)

            val backupObj: JSONObject = when {
                root.has("backup") -> {
                    val candidate = root.opt("backup")
                    if (candidate is JSONObject) {
                        candidate
                    } else {
                        return Result.failure(
                            IllegalArgumentException("The 'backup' field exists but is not a valid JSON object.")
                        )
                    }
                }
                manifestVersion != -1 -> {
                    return Result.failure(
                        IllegalArgumentException("Invalid iBackup format: missing required 'backup' field.")
                    )
                }
                else -> {
                    // Raw backup JSON provided directly without iBackup envelope
                    root
                }
            }

            val rawName = infoObj?.optString("name")?.trim().takeIf { !it.isNullOrBlank() }
                ?: sourceFileName?.substringBeforeLast('.')?.trim().takeIf { !it.isNullOrBlank() }
                ?: "converted_backup"

            val preview = calculateStats(
                backupObj = backupObj,
                backupName = rawName,
                mode = ConversionMode.IBACKUP_TO_JSON,
                author = infoObj?.optString("author") ?: "",
                version = infoObj?.optString("version") ?: "",
                releaseDate = infoObj?.optString("release_date") ?: "",
                changelog = infoObj?.optString("changelog") ?: ""
            )

            val formattedJson = backupObj.toString(2)
            val suggestedFileName = "${sanitizeFileName(rawName)}.json"

            Result.success(Triple(formattedJson, preview, suggestedFileName))
        } catch (e: JSONException) {
            Result.failure(IllegalArgumentException("Failed to parse input as valid JSON: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Converts a normal JSON backup file into a complete Instagram iBackup structure.
     * Generates standard manifest_version = 2, info metadata block, and wraps the
     * backup object accurately.
     */
    fun convertJsonToIbackup(
        inputContent: String,
        info: IBackupInfo,
        sourceFileName: String? = null
    ): Result<Triple<String, ConversionPreview, String>> {
        val trimmed = inputContent.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Input JSON content is empty."))
        }

        return try {
            val parsed = JSONObject(trimmed)

            // If user supplied an iBackup file with an existing "backup" key, extract it
            // to avoid nested "backup": { "backup": ... }
            val backupObj = if (parsed.has("backup") && parsed.optJSONObject("backup") != null) {
                parsed.getJSONObject("backup")
            } else {
                parsed
            }

            val root = JSONObject()
            root.put("manifest_version", 2)

            val infoObj = JSONObject()
            infoObj.put("id", info.id)
            infoObj.put("release_id", info.releaseId)
            infoObj.put("name", info.name)
            infoObj.put("author", info.author)
            infoObj.put("author_id", info.authorId)
            infoObj.put("version", info.version)
            infoObj.put("release_date", info.releaseDate)
            infoObj.put("changelog", info.changelog)

            root.put("info", infoObj)
            root.put("backup", backupObj)

            val preview = calculateStats(
                backupObj = backupObj,
                backupName = info.name,
                mode = ConversionMode.JSON_TO_IBACKUP,
                author = info.author,
                version = info.version,
                releaseDate = info.releaseDate,
                changelog = info.changelog
            )

            val formattedJson = root.toString(2)
            val suggestedFileName = "${sanitizeFileName(info.name)}.ibackup"

            Result.success(Triple(formattedJson, preview, suggestedFileName))
        } catch (e: JSONException) {
            Result.failure(IllegalArgumentException("Input is not a valid JSON object: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Computes statistics from the backup JSONObject:
     * - Total Universal Flags
     * - Total Sub-Flag Entries
     * - Total Backup Entries
     */
    fun calculateStats(
        backupObj: JSONObject,
        backupName: String,
        mode: ConversionMode,
        author: String = "",
        version: String = "",
        releaseDate: String = "",
        changelog: String = ""
    ): ConversionPreview {
        var universalFlagCount = 0
        var subFlagEntryCount = 0
        val totalBackupEntries = backupObj.length()

        val keys = backupObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = backupObj.opt(key)

            if (value is JSONArray) {
                if (key != "qe_overrides") {
                    universalFlagCount++
                }
                subFlagEntryCount += value.length()
            }
        }

        return ConversionPreview(
            backupName = backupName,
            totalUniversalFlags = universalFlagCount,
            totalSubFlagEntries = subFlagEntryCount,
            totalBackupEntries = totalBackupEntries,
            mode = mode,
            releaseVersion = version,
            author = author,
            releaseDate = releaseDate,
            changelog = changelog
        )
    }

    /**
     * Generates a UTC ISO-8601 string representation of the current moment.
     * e.g., 2026-09-05T15:59:38.718Z
     */
    fun generateUtcTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Generates a random UUID string for release_id.
     */
    fun generateReleaseId(): String = UUID.randomUUID().toString()

    /**
     * Generates an auto-increment or random positive Long identifier.
     */
    fun generateRandomId(): Long = Random.nextLong(100_000, 999_999_999)

    /**
     * Sanitizes a string into a safe file name.
     */
    fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return if (cleaned.isBlank()) "backup" else cleaned
    }
}
