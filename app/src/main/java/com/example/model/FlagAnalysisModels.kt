package com.example.model

import org.json.JSONObject

enum class BackupFileType {
    IBACKUP,
    JSON
}

/**
 * Filter mode for flag search and browsing in Flag Analysis.
 */
enum class FlagAnalysisFilter {
    ALL,
    ENABLED,
    DISABLED,
    MODIFIED,
    UNKNOWN
}

/**
 * Represents an analyzed subflag entry from the backup.
 */
data class AnalyzedSubFlag(
    val subCode: String,
    val name: String,
    val isUnknown: Boolean,
    val isEnabled: Boolean,
    val originalEnabled: Boolean,
    val rawEntryString: String
) {
    val isModified: Boolean
        get() = isEnabled != originalEnabled
}

/**
 * Represents an analyzed universal flag entry from the backup containing its subflags.
 */
data class AnalyzedUniversalFlag(
    val code: String,
    val rawKey: String,
    val name: String,
    val category: String,
    val isUnknown: Boolean,
    val subFlags: List<AnalyzedSubFlag>
) {
    val totalSubFlags: Int
        get() = subFlags.size

    val enabledSubFlags: Int
        get() = subFlags.count { it.isEnabled }

    val disabledSubFlags: Int
        get() = subFlags.count { !it.isEnabled }

    val hasUnknownSubFlags: Boolean
        get() = subFlags.any { it.isUnknown }

    val isModified: Boolean
        get() = subFlags.any { it.isModified }
}

/**
 * Represents complete parsed backup state in memory.
 * Preserves the exact original JSON objects for untouched metadata,
 * `qe_overrides`, and untouched flags.
 */
data class ParsedBackupState(
    val fileName: String,
    val fileType: BackupFileType,
    val rawRootJson: JSONObject,
    val rawBackupJson: JSONObject,
    val info: IBackupInfo? = null,
    val manifestVersion: Int? = null,
    val flags: List<AnalyzedUniversalFlag> = emptyList()
) {
    val totalUniversalFlags: Int
        get() = flags.size

    val totalSubFlags: Int
        get() = flags.sumOf { it.totalSubFlags }

    val totalEnabledSubFlags: Int
        get() = flags.sumOf { it.enabledSubFlags }

    val totalDisabledSubFlags: Int
        get() = flags.sumOf { it.disabledSubFlags }

    val totalUnknownFlags: Int
        get() = flags.count { it.isUnknown } + flags.sumOf { f -> f.subFlags.count { it.isUnknown } }

    val totalModifiedSubFlags: Int
        get() = flags.sumOf { f -> f.subFlags.count { it.isModified } }

    val enabledPercentage: Float
        get() = if (totalSubFlags == 0) 0f else (totalEnabledSubFlags.toFloat() / totalSubFlags.toFloat()) * 100f
}

/**
 * Compact summary stats displayed in the Flag Analysis dashboard.
 */
data class AnalysisDashboardStats(
    val totalUniversalFlags: Int,
    val totalSubFlags: Int,
    val enabledSubFlags: Int,
    val disabledSubFlags: Int,
    val unknownCount: Int,
    val modifiedCount: Int,
    val enabledPercentage: Float
)

/**
 * Breakdown of flags per category.
 */
data class CategoryAnalysisSummary(
    val category: String,
    val universalCount: Int,
    val subFlagCount: Int,
    val enabledCount: Int,
    val disabledCount: Int,
    val modifiedCount: Int
)

/**
 * Item used in change tracking and compare views.
 */
data class ModifiedFlagItem(
    val universalCode: String,
    val universalName: String,
    val subCode: String,
    val subName: String,
    val category: String,
    val originalState: Boolean,
    val currentState: Boolean
)

/**
 * Item for unmapped flags inspector.
 */
data class UnmappedFlagItem(
    val universalCode: String,
    val rawKey: String,
    val unmappedSubCodes: List<String>,
    val isUniversalUnmapped: Boolean
)
