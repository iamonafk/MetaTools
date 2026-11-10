package com.example.data

import com.example.model.CodeToNameResult
import com.example.model.ConversionAnalysisStats
import com.example.model.ConversionHistoryItem
import com.example.model.ConvertedSubFlag
import com.example.model.ConvertedUniversalFlag
import com.example.model.JsonFormatStyle
import com.example.model.UniversalFlag
import com.example.model.UnmappedCodeItem
import com.squareup.moshi.JsonReader
import okio.Buffer
import java.io.IOException

object JsonCodeToNameEngine {

    const val EXAMPLE_INPUT = """{
  "69718:": ["4: : true", "1: : true"],
  "53982:": ["0: : true"],
  "70831:": ["15: : true", "10: : true", "12: : true", "4: : false"],
  "102290:": ["0: : true", "1: : true"],
  "73763:": ["14: : true", "8: : true", "21: : true"],
  "64040:": ["1: : false", "2: : false"],
  "_qe_overrides_": []
}"""

    /**
     * Converts a code-based JSON string into a name-based JSON output using the provided mapping database.
     * Preserves original key order, non-flag fields, _qe_overrides_, empty arrays, and exact data values.
     */
    fun convert(
        inputJson: String,
        mappingFlags: List<UniversalFlag>
    ): Result<CodeToNameResult> {
        val trimmed = inputJson.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Input JSON is empty."))
        }

        return try {
            val rawMap = parseOrderedJson(trimmed)
            val mappingMap = mappingFlags.associateBy { it.code.trim() }

            val convertedFlags = mutableListOf<ConvertedUniversalFlag>()
            val unmappedItems = mutableListOf<UnmappedCodeItem>()
            val preservedRawEntries = LinkedHashMap<String, Any?>()
            val orderedConvertedMap = LinkedHashMap<String, Any?>()
            val seenUniversalCodes = mutableSetOf<String>()
            val errorMessages = mutableListOf<String>()

            var totalUniversalCount = 0
            var totalSubFlagCount = 0
            var mappedUniversalCount = 0
            var mappedSubFlagCount = 0
            var unmappedUniversalCount = 0
            var unmappedSubflagCount = 0
            var duplicateUniversalCount = 0
            var duplicateSubFlagCount = 0

            for ((key, value) in rawMap) {
                val cleanKey = key.trim()
                val potentialCode = cleanKey.trimEnd(':').trim()

                // Check if key is a universal flag code (numeric)
                val isUniversalCode = potentialCode.isNotEmpty() && potentialCode.all { it.isDigit() } &&
                    cleanKey != "_qe_overrides_" && cleanKey != "qe_overrides"

                if (isUniversalCode && value is List<*>) {
                    totalUniversalCount++

                    if (seenUniversalCodes.contains(potentialCode)) {
                        duplicateUniversalCount++
                    } else {
                        seenUniversalCodes.add(potentialCode)
                    }

                    val mappedUniversal = mappingMap[potentialCode]
                    val isUniversalMapped = mappedUniversal != null

                    if (isUniversalMapped) {
                        mappedUniversalCount++
                    } else {
                        unmappedUniversalCount++
                    }

                    val outputKey = if (isUniversalMapped) {
                        "$potentialCode:${mappedUniversal!!.name}"
                    } else {
                        cleanKey
                    }

                    val subFlagEntries = mutableListOf<ConvertedSubFlag>()
                    val outputSubFlagStrings = mutableListOf<String>()
                    val seenSubCodesInFlag = mutableSetOf<String>()

                    for (rawItem in value) {
                        if (rawItem == null) continue
                        val rawStr = rawItem.toString().trim()
                        totalSubFlagCount++

                        val (subCode, subValue) = parseSubFlagCodeAndValue(rawStr)

                        if (seenSubCodesInFlag.contains(subCode)) {
                            duplicateSubFlagCount++
                        } else {
                            seenSubCodesInFlag.add(subCode)
                        }

                        val mappedSub = mappedUniversal?.subFlags?.firstOrNull { it.code.trim() == subCode }
                        val isSubMapped = mappedSub != null

                        if (isSubMapped) {
                            mappedSubFlagCount++
                        } else {
                            unmappedSubflagCount++
                            unmappedItems.add(
                                UnmappedCodeItem(
                                    universalCode = potentialCode,
                                    universalName = mappedUniversal?.name,
                                    subFlagCode = subCode,
                                    rawEntry = rawStr,
                                    isUniversalUnmapped = !isUniversalMapped
                                )
                            )
                        }

                        val formattedSubString = if (isSubMapped) {
                            formatSubFlagOutput(subCode, mappedSub!!.name, subValue)
                        } else {
                            rawStr
                        }

                        subFlagEntries.add(
                            ConvertedSubFlag(
                                numericCode = subCode,
                                resolvedName = mappedSub?.name,
                                value = subValue,
                                originalEntryString = rawStr,
                                formattedOutputString = formattedSubString,
                                isMapped = isSubMapped
                            )
                        )
                        outputSubFlagStrings.add(formattedSubString)
                    }

                    convertedFlags.add(
                        ConvertedUniversalFlag(
                            numericCode = potentialCode,
                            resolvedName = mappedUniversal?.name,
                            originalKey = cleanKey,
                            formattedKey = outputKey,
                            subFlags = subFlagEntries,
                            isMapped = isUniversalMapped,
                            isDuplicate = duplicateUniversalCount > 0 && seenUniversalCodes.contains(potentialCode)
                        )
                    )

                    orderedConvertedMap[outputKey] = outputSubFlagStrings
                } else {
                    // Non-flag entry (e.g. "_qe_overrides_", "manifest_version", unknown objects)
                    preservedRawEntries[cleanKey] = value
                    orderedConvertedMap[cleanKey] = value
                }
            }

            val totalCodes = totalUniversalCount + totalSubFlagCount
            val totalMappedCodes = mappedUniversalCount + mappedSubFlagCount
            val coverage = if (totalCodes > 0) (totalMappedCodes.toFloat() / totalCodes.toFloat()) * 100f else 100f

            val stats = ConversionAnalysisStats(
                totalUniversalFlags = totalUniversalCount,
                totalSubFlags = totalSubFlagCount,
                mappedUniversalFlags = mappedUniversalCount,
                mappedSubFlags = mappedSubFlagCount,
                unmappedUniversalCodes = unmappedUniversalCount,
                unmappedSubflagCodes = unmappedSubflagCount,
                totalConvertedEntries = orderedConvertedMap.size,
                conversionErrors = errorMessages.size,
                duplicateUniversalCount = duplicateUniversalCount,
                duplicateSubFlagCount = duplicateSubFlagCount,
                mappingCoveragePercentage = coverage
            )

            val prettyJson = serializeOrderedJson(orderedConvertedMap, JsonFormatStyle.PRETTY)
            val compactJson = serializeOrderedJson(orderedConvertedMap, JsonFormatStyle.COMPACT)

            Result.success(
                CodeToNameResult(
                    convertedJsonPretty = prettyJson,
                    convertedJsonCompact = compactJson,
                    stats = stats,
                    convertedFlags = convertedFlags,
                    unmappedItems = unmappedItems,
                    preservedRawEntries = preservedRawEntries,
                    errorMessages = errorMessages
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to parse and convert JSON: ${e.message}"))
        }
    }

    /**
     * Extracts subflag code and preserved value from entry strings like:
     * "4: : true" -> Pair("4", "true")
     * "0: : 8000000" -> Pair("0", "8000000")
     * "3: : " -> Pair("3", "")
     * "0: transcoding_bitrate: 8000000" -> Pair("0", "8000000")
     * "1: false" -> Pair("1", "false")
     */
    fun parseSubFlagCodeAndValue(rawEntry: String): Pair<String, String> {
        val trimmed = rawEntry.trim()
        if (!trimmed.contains(':')) {
            return Pair(trimmed, "")
        }

        val subCode = trimmed.substringBefore(':').trim()
        val remainder = trimmed.substringAfter(':').trim()

        val value = if (remainder.contains(':')) {
            // e.g. ": true" or " transcoding_bitrate: 8000000" or ": "
            remainder.substringAfter(':').trim()
        } else {
            remainder.trim()
        }

        return Pair(subCode, value)
    }

    /**
     * Formats name-based subflag output:
     * e.g. "4: actual_subflag_name: true" or "3: direct_tab: "
     */
    fun formatSubFlagOutput(subCode: String, subFlagName: String, value: String): String {
        return if (value.isEmpty()) {
            "$subCode: $subFlagName: "
        } else {
            "$subCode: $subFlagName: $value"
        }
    }

    /**
     * Parses JSON string into an ordered LinkedHashMap using Moshi's JsonReader.
     */
    fun parseOrderedJson(jsonString: String): LinkedHashMap<String, Any?> {
        val buffer = Buffer().writeUtf8(jsonString)
        val reader = JsonReader.of(buffer)
        val result = readNextValue(reader)

        if (result is LinkedHashMap<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return result as LinkedHashMap<String, Any?>
        } else {
            throw IOException("Root of JSON must be an object { ... }")
        }
    }

    private fun readNextValue(reader: JsonReader): Any? {
        return when (reader.peek()) {
            JsonReader.Token.BEGIN_OBJECT -> {
                val map = LinkedHashMap<String, Any?>()
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    val value = readNextValue(reader)
                    map[name] = value
                }
                reader.endObject()
                map
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = mutableListOf<Any?>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(readNextValue(reader))
                }
                reader.endArray()
                list
            }
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> {
                val numStr = reader.nextString()
                numStr.toLongOrNull() ?: numStr.toDoubleOrNull() ?: numStr
            }
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    /**
     * Serializes an ordered LinkedHashMap into clean JSON (Pretty or Compact).
     */
    fun serializeOrderedJson(map: Map<String, Any?>, style: JsonFormatStyle): String {
        val sb = StringBuilder()
        serializeObject(map, sb, if (style == JsonFormatStyle.PRETTY) 0 else -1)
        return sb.toString()
    }

    private fun serializeObject(map: Map<String, Any?>, sb: StringBuilder, indent: Int) {
        val isPretty = indent >= 0
        sb.append("{")
        if (map.isEmpty()) {
            sb.append("}")
            return
        }

        if (isPretty) sb.append("\n")

        var first = true
        for ((key, value) in map) {
            if (!first) {
                sb.append(",")
                if (isPretty) sb.append("\n")
            }
            first = false

            if (isPretty) appendIndent(sb, indent + 1)
            sb.append("\"").append(escapeJson(key)).append("\":")
            if (isPretty) sb.append(" ")

            serializeValue(value, sb, if (isPretty) indent + 1 else -1)
        }

        if (isPretty) {
            sb.append("\n")
            appendIndent(sb, indent)
        }
        sb.append("}")
    }

    private fun serializeArray(list: List<*>, sb: StringBuilder, indent: Int) {
        val isPretty = indent >= 0
        sb.append("[")
        if (list.isEmpty()) {
            sb.append("]")
            return
        }

        if (isPretty) sb.append("\n")

        var first = true
        for (item in list) {
            if (!first) {
                sb.append(",")
                if (isPretty) sb.append("\n")
            }
            first = false

            if (isPretty) appendIndent(sb, indent + 1)
            serializeValue(item, sb, if (isPretty) indent + 1 else -1)
        }

        if (isPretty) {
            sb.append("\n")
            appendIndent(sb, indent)
        }
        sb.append("]")
    }

    private fun serializeValue(value: Any?, sb: StringBuilder, indent: Int) {
        when (value) {
            null -> sb.append("null")
            is Boolean -> sb.append(value)
            is Number -> sb.append(value)
            is String -> sb.append("\"").append(escapeJson(value)).append("\"")
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                serializeObject(value as Map<String, Any?>, sb, indent)
            }
            is List<*> -> serializeArray(value, sb, indent)
            else -> sb.append("\"").append(escapeJson(value.toString())).append("\"")
        }
    }

    private fun appendIndent(sb: StringBuilder, level: Int) {
        for (i in 0 until level) {
            sb.append("  ")
        }
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Looks up an individual universal or subflag numeric code in mapping flags.
     */
    fun lookupCode(code: String, mappingFlags: List<UniversalFlag>): List<Pair<String, String>> {
        val query = code.trim().lowercase()
        if (query.isEmpty()) return emptyList()

        val results = mutableListOf<Pair<String, String>>()
        for (flag in mappingFlags) {
            if (flag.code == query) {
                results.add(Pair("Universal Flag: ${flag.name}", "Code: ${flag.code} • Category: ${flag.category}"))
            }
            for (sub in flag.subFlags) {
                if (sub.code == query) {
                    results.add(Pair("Sub-Flag: ${sub.name}", "Code: ${sub.code} • Under Universal Flag: ${flag.name} (${flag.code})"))
                }
            }
        }
        return results
    }

    /**
     * Generates a text summary report of the conversion.
     */
    fun generateReportText(
        fileName: String,
        mappingSource: String,
        stats: ConversionAnalysisStats,
        unmappedItems: List<UnmappedCodeItem>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("==============================================")
        sb.appendLine("  MetaTools JSON Code-to-Name Conversion Report")
        sb.appendLine("==============================================")
        sb.appendLine("File: $fileName")
        sb.appendLine("Mapping Source: $mappingSource")
        sb.appendLine("Timestamp: ${java.util.Date()}")
        sb.appendLine("----------------------------------------------")
        sb.appendLine("Total Universal Flags: ${stats.totalUniversalFlags}")
        sb.appendLine("Mapped Universal Flags: ${stats.mappedUniversalFlags}")
        sb.appendLine("Unmapped Universal Flags: ${stats.unmappedUniversalCodes}")
        sb.appendLine("Total Sub-Flags: ${stats.totalSubFlags}")
        sb.appendLine("Mapped Sub-Flags: ${stats.mappedSubFlags}")
        sb.appendLine("Unmapped Sub-Flags: ${stats.unmappedSubflagCodes}")
        sb.appendLine("Mapping Coverage: ${String.format("%.1f", stats.mappingCoveragePercentage)}%")
        sb.appendLine("Duplicate Universal Flags: ${stats.duplicateUniversalCount}")
        sb.appendLine("Duplicate Sub-Flags: ${stats.duplicateSubFlagCount}")
        sb.appendLine("----------------------------------------------")

        if (unmappedItems.isNotEmpty()) {
            sb.appendLine("UNMAPPED CODES (${unmappedItems.size}):")
            for (item in unmappedItems) {
                if (item.isUniversalUnmapped) {
                    sb.appendLine("• Universal Flag Code ${item.universalCode}: unmapped (Entry: ${item.rawEntry})")
                } else {
                    sb.appendLine("• Sub-Flag Code ${item.subFlagCode} under ${item.universalName ?: item.universalCode}: unmapped")
                }
            }
        } else {
            sb.appendLine("All universal flags and sub-flags were successfully mapped!")
        }

        return sb.toString()
    }
}
