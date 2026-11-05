package com.example.data

import com.example.model.FlagParameter
import com.example.model.ParsedFlagItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object FlagInformationParser {

    const val EXAMPLE_INPUT = """Application: Instagram

Universe: modern_ui_edge_to_edge

Stable Id: 126024

Parameters:

is_bottom_enabled_for_main_activity: false (Sub Flag)

Use: Disable this subflag to fix the Reels playback bug where pausing a Reel freezes its thumbnail on screen. After resuming, audio plays but the video remains frozen or blurred instead of continuing normally.

Instagram Version: —435.0.0.37.76"""

    private val HEADER_APPLICATION = Regex("""(?i)^\s*(Application|App)\s*:\s*(.*)$""")
    private val HEADER_UNIVERSE = Regex("""(?i)^\s*(Universe|Universal\s+Flag|Flag\s+Name)\s*:\s*(.*)$""")
    private val HEADER_STABLE_ID = Regex("""(?i)^\s*(Stable\s*Id|Stable\s*ID|StableId)\s*:\s*(.*)$""")
    private val HEADER_PARAMETERS = Regex("""(?i)^\s*(Parameters|Params|Parameter)\s*:\s*(.*)$""")
    private val HEADER_USE = Regex("""(?i)^\s*(Use|Usage|Description|Fix)\s*:\s*(.*)$""")
    private val HEADER_VERSION = Regex("""(?i)^\s*((?:Instagram|Threads|App)?\s*Version)\s*:\s*(.*)$""")

    /**
     * Parses raw input text into a list of [ParsedFlagItem]s.
     * Supports single flag or multiple pasted flags separated by dividers or repeating headers.
     */
    fun parse(rawText: String): List<ParsedFlagItem> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Split multiple flags if present
        val blocks = splitIntoBlocks(trimmed)
        val parsedList = blocks.mapNotNull { block ->
            parseSingleBlock(block)
        }

        return markDuplicates(parsedList)
    }

    /**
     * Splits text into individual flag blocks based on divider markers or repeating Application/Universe headers.
     */
    private fun splitIntoBlocks(text: String): List<String> {
        val lines = text.lines()
        val blocks = mutableListOf<String>()
        val currentBlock = StringBuilder()

        for (line in lines) {
            val trimmedLine = line.trim()

            // Check if line is an explicit divider (e.g. ---, ===, ___)
            val isDivider = trimmedLine.matches(Regex("""^[-=_*]{3,}$"""))

            // Check if a new flag block is starting (repeating Application: or Universe:)
            val blockStr = currentBlock.toString()
            val isNewFlagStart = (HEADER_APPLICATION.containsMatchIn(trimmedLine) && HEADER_APPLICATION.containsMatchIn(blockStr)) ||
                (HEADER_UNIVERSE.containsMatchIn(trimmedLine) && HEADER_UNIVERSE.containsMatchIn(blockStr))

            if (isDivider || isNewFlagStart) {
                if (currentBlock.isNotBlank()) {
                    blocks.add(currentBlock.toString().trim())
                    currentBlock.clear()
                }
                if (!isDivider) {
                    currentBlock.appendLine(line)
                }
            } else {
                currentBlock.appendLine(line)
            }
        }

        if (currentBlock.isNotBlank()) {
            blocks.add(currentBlock.toString().trim())
        }

        return if (blocks.isEmpty()) listOf(text) else blocks
    }

    /**
     * Parses a single flag text block into a [ParsedFlagItem].
     */
    private fun parseSingleBlock(blockText: String): ParsedFlagItem? {
        val lines = blockText.lines()
        var app = ""
        var universe = ""
        var stableId = ""
        val parameterLines = mutableListOf<String>()
        val useLines = mutableListOf<String>()
        var version = ""
        var versionLabel = "Instagram Version"

        var currentSection: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                // Keep empty lines in use description if we're inside Use section
                if (currentSection == "USE" && useLines.isNotEmpty()) {
                    useLines.add("")
                }
                continue
            }

            // Check headers
            val appMatch = HEADER_APPLICATION.matchEntire(trimmed)
            if (appMatch != null) {
                app = appMatch.groupValues[2].trim()
                currentSection = "APP"
                continue
            }

            val universeMatch = HEADER_UNIVERSE.matchEntire(trimmed)
            if (universeMatch != null) {
                universe = universeMatch.groupValues[2].trim()
                currentSection = "UNIVERSE"
                continue
            }

            val stableIdMatch = HEADER_STABLE_ID.matchEntire(trimmed)
            if (stableIdMatch != null) {
                stableId = stableIdMatch.groupValues[2].trim()
                currentSection = "STABLE_ID"
                continue
            }

            val paramMatch = HEADER_PARAMETERS.matchEntire(trimmed)
            if (paramMatch != null) {
                currentSection = "PARAMETERS"
                val inlineParam = paramMatch.groupValues[2].trim()
                if (inlineParam.isNotEmpty()) {
                    parameterLines.add(inlineParam)
                }
                continue
            }

            val useMatch = HEADER_USE.matchEntire(trimmed)
            if (useMatch != null) {
                currentSection = "USE"
                val inlineUse = useMatch.groupValues[2].trim()
                if (inlineUse.isNotEmpty()) {
                    useLines.add(inlineUse)
                }
                continue
            }

            val versionMatch = HEADER_VERSION.matchEntire(trimmed)
            if (versionMatch != null) {
                currentSection = "VERSION"
                versionLabel = versionMatch.groupValues[1].trim()
                version = versionMatch.groupValues[2].trim()
                continue
            }

            // If not a new header, append to the active section
            when (currentSection) {
                "PARAMETERS" -> parameterLines.add(trimmed)
                "USE" -> useLines.add(trimmed)
                "APP" -> if (app.isEmpty()) app = trimmed
                "UNIVERSE" -> if (universe.isEmpty()) universe = trimmed
                "STABLE_ID" -> if (stableId.isEmpty()) stableId = trimmed
                "VERSION" -> if (version.isEmpty()) version = trimmed
                else -> {
                    // Fallback heuristics: check if line looks like "key: value" parameter
                    if (trimmed.contains(':') && universe.isNotEmpty() && parametersLookalike(trimmed)) {
                        parameterLines.add(trimmed)
                    }
                }
            }
        }

        // Clean use description (trim excessive trailing/leading blank lines)
        val useDescription = useLines.joinToString("\n").trim()

        // Parse individual parameters
        val parsedParams = parameterLines.map { line ->
            parseParameterLine(line)
        }

        // If block is totally empty with no recognized data, return null
        if (app.isEmpty() && universe.isEmpty() && stableId.isEmpty() && parsedParams.isEmpty() && useDescription.isEmpty() && version.isEmpty()) {
            return null
        }

        return ParsedFlagItem(
            application = app,
            universe = universe,
            stableId = stableId,
            parameters = parsedParams,
            useDescription = useDescription,
            appVersion = version,
            versionLabel = versionLabel,
            rawPastedText = blockText.trim()
        )
    }

    /**
     * Parses a single parameter line, e.g.:
     * "is_bottom_enabled_for_main_activity: false (Sub Flag)"
     * "max_duration_seconds: 60"
     */
    fun parseParameterLine(line: String): FlagParameter {
        val trimmed = line.trim()

        // Check if marked as Sub Flag
        val subFlagRegex = Regex("""(?i)\s*\((?:sub\s*flag|subflag)\)""")
        val hasSubFlagTag = subFlagRegex.containsMatchIn(trimmed)
        val cleanedLine = trimmed.replace(subFlagRegex, "").trim()

        return if (cleanedLine.contains(':')) {
            val parts = cleanedLine.split(':', limit = 2)
            val name = parts[0].trim()
            val value = parts.getOrNull(1)?.trim() ?: ""
            FlagParameter(
                name = name,
                value = value,
                isSubFlag = hasSubFlagTag,
                rawText = trimmed
            )
        } else {
            FlagParameter(
                name = cleanedLine,
                value = "",
                isSubFlag = hasSubFlagTag,
                rawText = trimmed
            )
        }
    }

    private fun parametersLookalike(line: String): Boolean {
        val lower = line.lowercase(Locale.ROOT)
        return lower.contains("true") || lower.contains("false") || lower.contains("sub flag") ||
            lower.contains("is_") || lower.contains("enable") || lower.contains("timeout") || lower.contains("count")
    }

    /**
     * Detects duplicate Universe names and Stable IDs across multiple flags.
     */
    fun markDuplicates(flags: List<ParsedFlagItem>): List<ParsedFlagItem> {
        val universeCounts = mutableMapOf<String, Int>()
        val stableIdCounts = mutableMapOf<String, Int>()

        for (flag in flags) {
            val uNorm = flag.universe.trim().lowercase(Locale.ROOT)
            if (uNorm.isNotEmpty()) {
                universeCounts[uNorm] = (universeCounts[uNorm] ?: 0) + 1
            }
            val idNorm = flag.stableId.trim()
            if (idNorm.isNotEmpty()) {
                stableIdCounts[idNorm] = (stableIdCounts[idNorm] ?: 0) + 1
            }
        }

        return flags.map { flag ->
            val uNorm = flag.universe.trim().lowercase(Locale.ROOT)
            val idNorm = flag.stableId.trim()
            val isDupU = uNorm.isNotEmpty() && (universeCounts[uNorm] ?: 0) > 1
            val isDupId = idNorm.isNotEmpty() && (stableIdCounts[idNorm] ?: 0) > 1

            flag.copy(
                isDuplicateUniverse = isDupU,
                isDuplicateStableId = isDupId
            )
        }
    }

    /**
     * Cleans inconsistent spacing, trims extraneous spaces, and standardizes field order.
     */
    fun autoFormatText(rawText: String): String {
        val parsed = parse(rawText)
        if (parsed.isEmpty()) return rawText.trim()

        return parsed.joinToString("\n\n---\n\n") { it.toStandardTextFormat() }
    }

    /**
     * Converts parsed flags into a clean JSON representation.
     */
    fun exportToJson(flags: List<ParsedFlagItem>): String {
        val jsonArray = JSONArray()

        for (flag in flags) {
            val obj = JSONObject()
            obj.put("application", flag.application)
            obj.put("universe", flag.universe)
            obj.put("stable_id", flag.stableId)
            obj.put("flag_type", flag.detectedType.displayName)

            val paramsObj = JSONObject()
            val paramsArray = JSONArray()

            for (p in flag.parameters) {
                val pObj = JSONObject()
                pObj.put("name", p.name)
                pObj.put("value", p.value)
                pObj.put("is_sub_flag", p.isSubFlag)
                paramsArray.put(pObj)

                if (p.name.isNotEmpty()) {
                    paramsObj.put(p.name, p.value)
                }
            }

            obj.put("parameters", paramsObj)
            obj.put("parameters_detailed", paramsArray)
            obj.put("use", flag.useDescription)
            obj.put("version_label", flag.versionLabel)
            obj.put("version", flag.appVersion)

            jsonArray.put(obj)
        }

        return jsonArray.toString(2)
    }
}
