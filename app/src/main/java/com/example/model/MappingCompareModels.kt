package com.example.model

import android.net.Uri

sealed class CompareSource {
    data object BuiltIn : CompareSource() {
        val displayName: String = "Built-in (Askxtylishh.json)"
    }
    data class ActiveCustom(val name: String) : CompareSource() {
        val displayName: String = "Active Custom ($name)"
    }
    data class DeviceFile(val uri: Uri, val name: String) : CompareSource() {
        val displayName: String = name
    }
    data class PastedJson(
        val rawJson: String,
        val name: String = "Pasted JSON"
    ) : CompareSource() {
        val displayName: String = "$name (Pasted)"
    }

    fun getLabel(): String = when (this) {
        is BuiltIn -> displayName
        is ActiveCustom -> displayName
        is DeviceFile -> displayName
        is PastedJson -> displayName
    }
}

enum class DiagnosticIssueType {
    DUPLICATE_UNIVERSAL_NAME,
    DUPLICATE_UNIVERSAL_CODE,
    DUPLICATE_SUBFLAG,
    MALFORMED_ENTRY,
    MISSING_NAME,
    UNKNOWN_CATEGORY
}

data class DiagnosticIssue(
    val type: DiagnosticIssueType,
    val entryIndex: Int,
    val rawEntry: String,
    val flagName: String = "",
    val flagCode: String = "",
    val reason: String
)

data class MappingParseDiagnostics(
    val sourceName: String,
    val validFlags: List<UniversalFlag>,
    val rawEntryCount: Int,
    val validEntryCount: Int,
    val totalSubFlagsCount: Int,
    val byteSize: Long,
    val previewEntries: List<String>,
    val duplicateUniversalEntries: List<DiagnosticIssue>,
    val duplicateSubFlagEntries: List<DiagnosticIssue>,
    val malformedEntries: List<DiagnosticIssue>,
    val missingNameEntries: List<DiagnosticIssue>,
    val unknownCategoryEntries: List<DiagnosticIssue>,
    val isValid: Boolean,
    val validationError: String? = null
) {
    val duplicateCount: Int
        get() = duplicateUniversalEntries.size + duplicateSubFlagEntries.size

    val invalidCount: Int
        get() = malformedEntries.size + missingNameEntries.size

    val unknownCategoryCount: Int
        get() = unknownCategoryEntries.size

    val healthScore: Int
        get() {
            if (!isValid || validEntryCount == 0) return 0
            val totalObserved = (rawEntryCount.coerceAtLeast(1)).toFloat()
            val invalidPenalty = (malformedEntries.size + missingNameEntries.size) / totalObserved * 45f
            val dupPenalty = (duplicateUniversalEntries.size + duplicateSubFlagEntries.size) / totalObserved * 25f
            val unknownPenalty = unknownCategoryEntries.size / totalObserved * 20f
            return (100f - invalidPenalty - dupPenalty - unknownPenalty).toInt().coerceIn(0, 100)
        }
}

data class SourcePreviewInfo(
    val sourceName: String,
    val isValid: Boolean,
    val validationMessage: String,
    val universalFlagCount: Int,
    val subFlagCount: Int,
    val byteSize: Long,
    val duplicateCount: Int,
    val invalidCount: Int,
    val previewEntries: List<String>
)

data class NewFlagItem(
    val flag: UniversalFlag
)

data class NewSubFlagItem(
    val parentFlagName: String,
    val parentFlagCode: String,
    val category: String,
    val subFlag: SubFlag
)

data class RemovedFlagItem(
    val flag: UniversalFlag
)

data class RemovedSubFlagItem(
    val parentFlagName: String,
    val parentFlagCode: String,
    val category: String,
    val subFlag: SubFlag
)

data class UpdatedFlagCodeItem(
    val flagName: String,
    val category: String,
    val oldCode: String,
    val newCode: String
)

data class UpdatedSubFlagItem(
    val parentFlagName: String,
    val category: String,
    val subFlagName: String,
    val oldCode: String,
    val newCode: String
)

data class UnchangedFlagItem(
    val flag: UniversalFlag
)

data class PotentialRenameCandidate(
    val scope: String, // "UNIVERSAL" or "SUBFLAG"
    val sharedCode: String,
    val oldName: String,
    val newName: String,
    val parentFlagName: String? = null,
    val category: String
)

enum class CatalogAvailability(val label: String) {
    OLD_ONLY("Old Mapping Only"),
    NEW_ONLY("New Mapping Only"),
    BOTH_IDENTICAL("Both Files (Identical)"),
    BOTH_UPDATED("Both Files (Updated)")
}

enum class FlagChangeStatus(val label: String) {
    NEW("New"),
    REMOVED("Removed"),
    UPDATED("Updated"),
    UNCHANGED("Unchanged")
}

data class CatalogSubFlagDetail(
    val name: String,
    val oldCode: String?,
    val newCode: String?,
    val status: FlagChangeStatus
)

data class CatalogFlagItem(
    val name: String,
    val oldCode: String?,
    val newCode: String?,
    val category: String,
    val availability: CatalogAvailability,
    val status: FlagChangeStatus,
    val subFlags: List<CatalogSubFlagDetail>,
    val oldSubFlagCount: Int,
    val newSubFlagCount: Int,
    val isDuplicateInOld: Boolean = false,
    val isDuplicateInNew: Boolean = false,
    val isUnknownCategory: Boolean = false
) {
    val displayCode: String get() = newCode ?: oldCode ?: ""
    val totalDistinctSubFlags: Int get() = subFlags.size
}

data class CategoryAnalyticsItem(
    val categoryName: String,
    val totalUniqueFlags: Int,
    val oldCount: Int,
    val newCount: Int,
    val newFlagsCount: Int,
    val removedFlagsCount: Int,
    val updatedFlagsCount: Int,
    val unchangedFlagsCount: Int,
    val totalSubFlags: Int,
    val unchangedPercent: Int
)

data class ComparisonSummary(
    val oldSourceName: String,
    val newSourceName: String,
    val totalOldFlags: Int,
    val totalNewFlags: Int,
    val totalOldSubFlags: Int = 0,
    val totalNewSubFlags: Int = 0,
    val newFlagsCount: Int, // New Universal + New Subflags (kept for compatibility)
    val removedFlagsCount: Int, // Removed Universal + Removed Subflags (kept for compatibility)
    val updatedFlagsCount: Int, // Updated Flag Codes + Updated Subflag Codes (kept for compatibility)
    val unchangedFlagsCount: Int,
    val newUniversalFlagsCount: Int = 0,
    val removedUniversalFlagsCount: Int = 0,
    val updatedUniversalFlagsCount: Int = 0,
    val newSubFlagsCount: Int = 0,
    val removedSubFlagsCount: Int = 0,
    val updatedSubFlagsCount: Int = 0,
    val duplicateEntriesCount: Int = 0,
    val invalidEntriesCount: Int = 0,
    val unknownCategoryCount: Int = 0,
    val potentialRenamesCount: Int = 0,
    val oldByteSize: Long = 0L,
    val newByteSize: Long = 0L,
    val oldHealthScore: Int = 100,
    val newHealthScore: Int = 100,
    val timestamp: Long = System.currentTimeMillis()
)

data class ComparisonReport(
    val summary: ComparisonSummary,
    val newFlags: List<NewFlagItem>,
    val newSubFlags: List<NewSubFlagItem>,
    val removedFlags: List<RemovedFlagItem>,
    val removedSubFlags: List<RemovedSubFlagItem>,
    val updatedFlagCodes: List<UpdatedFlagCodeItem>,
    val updatedSubFlags: List<UpdatedSubFlagItem>,
    val unchangedFlags: List<UnchangedFlagItem> = emptyList(),
    val potentialRenames: List<PotentialRenameCandidate> = emptyList(),
    val catalogFlags: List<CatalogFlagItem> = emptyList(),
    val categoryAnalytics: List<CategoryAnalyticsItem> = emptyList(),
    val oldDiagnostics: MappingParseDiagnostics? = null,
    val newDiagnostics: MappingParseDiagnostics? = null
) {
    val totalNewCount: Int get() = newFlags.size + newSubFlags.size
    val totalRemovedCount: Int get() = removedFlags.size + removedSubFlags.size
    val totalUpdatedCount: Int get() = updatedFlagCodes.size + updatedSubFlags.size
}

enum class CompareFilterTab {
    ALL,
    NEW,
    REMOVED,
    UPDATED,
    UNCHANGED
}

enum class CompareQuickFilter(val label: String) {
    ALL("All"),
    NEW("New"),
    REMOVED("Removed"),
    UPDATED("Updated"),
    UNCHANGED("Unchanged"),
    HAS_SUBFLAGS("Has Subflags"),
    NO_SUBFLAGS("No Subflags"),
    DUPLICATE("Duplicate"),
    INVALID("Invalid"),
    UNKNOWN_CATEGORY("Unknown Category")
}

enum class CompareSectionTab(val label: String) {
    CHANGES("Changes"),
    CATALOG("Catalog"),
    ANALYTICS("Analytics"),
    HEALTH("Health"),
    HISTORY("History")
}

enum class CatalogSortOption(val label: String) {
    ALPHABETICAL("Alphabetical"),
    CODE("Flag Code"),
    CATEGORY("Category"),
    SUBFLAG_COUNT("Subflag Count"),
    STATUS("Change Status")
}

enum class CatalogScopeFilter(val label: String) {
    ALL("All Flags"),
    UNIVERSAL_ONLY("Universal Only"),
    SUBFLAGS_ONLY("Subflags Only")
}

enum class ExportReportType(
    val title: String,
    val defaultFileName: String,
    val description: String
) {
    COMPLETE_COMPARISON(
        title = "Complete Comparison Report",
        defaultFileName = "Mapping_Comparison_Report",
        description = "Summary, new, removed, updated, unchanged flags, and diagnostics"
    ),
    NEW_ONLY(
        title = "New Flags Report",
        defaultFileName = "New_Flags_Report",
        description = "Only newly added universal flags and new subflags"
    ),
    REMOVED_ONLY(
        title = "Removed Flags Report",
        defaultFileName = "Removed_Flags_Report",
        description = "Only removed universal flags and removed subflags"
    ),
    UPDATED_ONLY(
        title = "Updated Flags Report",
        defaultFileName = "Updated_Flags_Report",
        description = "Universal flags and subflags with changed codes or modified subflags"
    ),
    COMPLETE_CATALOG(
        title = "Complete Flag Catalog",
        defaultFileName = "Complete_Flag_Catalog",
        description = "Full merged catalog with Old/New codes, availability, and subflags"
    ),
    CATEGORY_CATALOG(
        title = "Category-Wise Catalog",
        defaultFileName = "Category_Wise_Catalog",
        description = "All 66 MetaTools categories with counts and categorized flags"
    ),
    UNMAPPED_INVALID(
        title = "Unmapped & Invalid Entries",
        defaultFileName = "Unmapped_Invalid_Entries",
        description = "Duplicates, malformed lines, missing names, and Other category entries"
    )
}

enum class ExportFileFormat(val extension: String, val mimeType: String, val label: String) {
    JSON("json", "application/json", "JSON (.json)"),
    TXT("txt", "text/plain", "Text (.txt)")
}

data class CompareHistoryRecord(
    val id: String,
    val oldSourceName: String,
    val newSourceName: String,
    val totalOldFlags: Int,
    val totalNewFlags: Int,
    val newCount: Int,
    val removedCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int,
    val healthScore: Int,
    val timestamp: Long,
    val hasSnapshotFile: Boolean = false
)
