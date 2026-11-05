package com.example.model

import java.util.UUID

/**
 * Detected category of the flag based on parameter metadata and contents.
 */
enum class DetectedFlagType(val displayName: String) {
    UNIVERSAL_FLAG("Universal Flag"),
    SUB_FLAG("Sub Flag"),
    MULTIPLE_PARAMETERS("Multiple Parameters")
}

/**
 * Individual parameter associated with a flag.
 */
data class FlagParameter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val value: String,
    val isSubFlag: Boolean = false,
    val rawText: String = ""
) {
    /**
     * Formats this parameter in the standard format:
     * "is_bottom_enabled_for_main_activity: false (Sub Flag)" or "key: value"
     */
    fun toFormattedString(): String {
        return if (rawText.isNotBlank()) {
            rawText.trim()
        } else {
            val subFlagTag = if (isSubFlag) " (Sub Flag)" else ""
            if (value.isNotBlank()) "$name: $value$subFlagTag" else name + subFlagTag
        }
    }
}

/**
 * Represents a complete parsed flag information entry.
 */
data class ParsedFlagItem(
    val id: String = UUID.randomUUID().toString(),
    val application: String = "",
    val universe: String = "",
    val stableId: String = "",
    val parameters: List<FlagParameter> = emptyList(),
    val useDescription: String = "",
    val appVersion: String = "",
    val versionLabel: String = "Instagram Version",
    val rawPastedText: String = "",
    val isDuplicateUniverse: Boolean = false,
    val isDuplicateStableId: Boolean = false
) {
    val detectedType: DetectedFlagType
        get() = when {
            parameters.size > 1 -> DetectedFlagType.MULTIPLE_PARAMETERS
            parameters.any { it.isSubFlag } -> DetectedFlagType.SUB_FLAG
            else -> DetectedFlagType.UNIVERSAL_FLAG
        }

    val missingFields: List<String>
        get() {
            val list = mutableListOf<String>()
            if (application.isBlank()) list.add("Application")
            if (universe.isBlank()) list.add("Universe")
            if (stableId.isBlank()) list.add("Stable Id")
            if (parameters.isEmpty()) list.add("Parameters")
            if (useDescription.isBlank()) list.add("Use")
            if (appVersion.isBlank()) list.add("Version")
            return list
        }

    /**
     * Reconstructs the complete flag in standard clean plain text format.
     * Preserves standard order:
     * Application:
     * Universe:
     * Stable Id:
     * Parameters:
     * Use:
     * Instagram Version:
     */
    fun toStandardTextFormat(): String {
        val lines = mutableListOf<String>()

        if (application.isNotBlank()) {
            lines.add("Application: $application")
            lines.add("")
        }

        if (universe.isNotBlank()) {
            lines.add("Universe: $universe")
            lines.add("")
        }

        if (stableId.isNotBlank()) {
            lines.add("Stable Id: $stableId")
            lines.add("")
        }

        if (parameters.isNotEmpty()) {
            lines.add("Parameters:")
            parameters.forEach { param ->
                lines.add(param.toFormattedString())
            }
            lines.add("")
        }

        if (useDescription.isNotBlank()) {
            lines.add("Use: $useDescription")
            lines.add("")
        }

        if (appVersion.isNotBlank()) {
            val label = versionLabel.ifBlank { "Instagram Version" }
            lines.add("$label: $appVersion")
        }

        return lines.joinToString("\n").trim()
    }
}
