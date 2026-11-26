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
    EMPTY_SUBFLAG,
    SUBFLAG_CODE_CONFLICT,
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

data class DuplicateCleanupSuggestion(
    val sourceSide: String,
    val flagName: String,
    val flagCode: String,
    val suggestionType: String,
    val recommendation: String,
    val rawEntry: String
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
    val validationError: String? = null,
    val emptySubFlagEntries: List<DiagnosticIssue> = emptyList(),
    val subFlagConsistencyIssues: List<DiagnosticIssue> = emptyList()
) {
    val duplicateCount: Int
        get() = duplicateUniversalEntries.size + duplicateSubFlagEntries.size

    val invalidCount: Int
        get() = malformedEntries.size + missingNameEntries.size + emptySubFlagEntries.size

    val unknownCategoryCount: Int
        get() = unknownCategoryEntries.size

    val completenessPercent: Int
        get() {
            if (!isValid || rawEntryCount <= 0) return 0
            val validRatio = (validEntryCount.toFloat() / rawEntryCount.toFloat()) * 100f
            return validRatio.toInt().coerceIn(0, 100)
        }

    val healthScore: Int
        get() {
            if (!isValid || validEntryCount == 0) return 0
            val totalObserved = (rawEntryCount.coerceAtLeast(1)).toFloat()
            val invalidPenalty = invalidCount / totalObserved * 45f
            val dupPenalty = duplicateCount / totalObserved * 25f
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

data class ParentChildChangeItem(
    val subFlagName: String,
    val oldParentFlagName: String,
    val oldParentCode: String,
    val newParentFlagName: String,
    val newParentCode: String,
    val oldSubCode: String,
    val newSubCode: String,
    val category: String
)

data class PotentialRenameCandidate(
    val scope: String, // "UNIVERSAL" or "SUBFLAG"
    val sharedCode: String,
    val oldName: String,
    val newName: String,
    val parentFlagName: String? = null,
    val category: String
)

data class NamespaceStatItem(
    val prefix: String,
    val oldCount: Int,
    val newCount: Int,
    val addedCount: Int,
    val removedCount: Int,
    val growthPercent: Float,
    val isThreadsP92: Boolean = false
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

enum class CatalogDisplayMode(val label: String) {
    EXPANDED("Expanded"),
    COMPACT("Compact")
}

enum class CompareMode(val label: String) {
    TWO_FILE("2-File Diff"),
    MULTI_FILE("Multi-File (3+)"),
    SESSION_DIFF("Session Diff")
}

data class CompareSourceSlot(
    val id: String,
    val label: String,
    val source: CompareSource? = null,
    val preview: SourcePreviewInfo? = null
)

data class MultiFileFlagPresence(
    val flagName: String,
    val category: String,
    val namespacePrefix: String,
    val codesBySource: Map<String, String?>,
    val subflagCountsBySource: Map<String, Int>,
    val presentInSourceLabels: List<String>,
    val distinctCodesCount: Int,
    val subFlagNames: List<String> = emptyList()
)

data class MultiFileComparisonReport(
    val sourceNames: List<String>,
    val sourcePreviews: List<SourcePreviewInfo>,
    val uniqueFlagsTotal: Int,
    val commonInAllCount: Int,
    val onlyInOneFileCount: Int,
    val codeDivergentCount: Int,
    val entries: List<MultiFileFlagPresence>,
    val baselineVsLatestReport: ComparisonReport?,
    val pairwiseDeltas: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class SessionDiffItem(
    val flagName: String,
    val category: String,
    val statusInA: String,
    val statusInB: String
)

data class SessionDiffReport(
    val sessionALabel: String,
    val sessionBLabel: String,
    val newlyAppearedInB: List<String>,
    val disappearedInB: List<String>,
    val statusTransitions: List<SessionDiffItem>,
    val deltaNewCount: Int,
    val deltaRemovedCount: Int,
    val deltaUpdatedCount: Int
)

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
    val isUnknownCategory: Boolean = false,
    val oldOriginalEntry: String? = null,
    val newOriginalEntry: String? = null
) {
    val displayCode: String get() = newCode ?: oldCode ?: ""
    val totalDistinctSubFlags: Int get() = subFlags.size
    val namespacePrefix: String
        get() {
            val idx = name.indexOf('_')
            return if (idx in 1..14) "${name.substring(0, idx)}_" else "other"
        }
    val isOnlyInOneMapping: Boolean
        get() = availability == CatalogAvailability.OLD_ONLY || availability == CatalogAvailability.NEW_ONLY
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
    val unchangedPercent: Int,
    val oldSubFlagsCount: Int = 0,
    val newSubFlagsCount: Int = 0,
    val flagGrowthPercent: Float = 0f,
    val subflagGrowthPercent: Float = 0f
)

data class ComparisonSummary(
    val oldSourceName: String,
    val newSourceName: String,
    val totalOldFlags: Int,
    val totalNewFlags: Int,
    val totalOldSubFlags: Int = 0,
    val totalNewSubFlags: Int = 0,
    val newFlagsCount: Int,
    val removedFlagsCount: Int,
    val updatedFlagsCount: Int,
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
    val timestamp: Long = System.currentTimeMillis(),
    val mappingGrowthPercent: Float = 0f,
    val subflagGrowthPercent: Float = 0f,
    val parentChildChangesCount: Int = 0,
    val emptySubflagEntriesCount: Int = 0,
    val threadsP92FlagsCount: Int = 0
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
    val newDiagnostics: MappingParseDiagnostics? = null,
    val parentChildChanges: List<ParentChildChangeItem> = emptyList(),
    val namespaceStats: List<NamespaceStatItem> = emptyList(),
    val cleanupSuggestions: List<DuplicateCleanupSuggestion> = emptyList()
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

/**
 * 15 dedicated organization sections for Mapping File Compare & Catalog Analyzer.
 */
enum class CompareResultSection(val label: String) {
    OVERVIEW("Overview"),
    COMPLETE_CATALOG("Complete Catalog"),
    NEW_FLAGS("New Flags"),
    REMOVED_FLAGS("Removed Flags"),
    UPDATED_FLAGS("Updated Flags"),
    UNCHANGED_FLAGS("Unchanged Flags"),
    NEW_SUBFLAGS("New Subflags"),
    REMOVED_SUBFLAGS("Removed Subflags"),
    UPDATED_SUBFLAGS("Updated Subflags"),
    CATEGORIES("Categories"),
    DUPLICATES("Duplicates"),
    INVALID_ENTRIES("Invalid Entries"),
    UNKNOWN_CATEGORIES("Unknown Categories"),
    MAPPING_HEALTH("Mapping Health"),
    EXPORT_REPORTS("Export Reports")
}

enum class CatalogSortOption(val label: String) {
    ALPHABETICAL("Alphabetical Flag"),
    ALPHA_SUBFLAG("Alphabetical Subflag"),
    CODE("Universal Code"),
    SUBFLAG_CODE("Subflag Code"),
    SUBFLAG_COUNT("Highest Subflag Count"),
    LOWEST_SUBFLAG_COUNT("Lowest Subflag Count"),
    CATEGORY("Category"),
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
        description = "Summary, new, removed, updated, unchanged flags, parent-child shifts, and diagnostics"
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
        description = "All 66 MetaTools categories with counts, growth %, and categorized flags"
    ),
    SINGLE_CATEGORY_EXPORT(
        title = "Selected Category Catalog",
        defaultFileName = "Category_Export",
        description = "Export only the currently selected category from the catalog"
    ),
    MAPPING_HEALTH_REPORT(
        title = "Mapping Health Report",
        defaultFileName = "Mapping_Health_Report",
        description = "Completeness %, uniqueness validation, namespaces, P92/Threads & structure checks"
    ),
    UNMAPPED_INVALID(
        title = "Duplicates & Invalid Entries",
        defaultFileName = "Duplicates_Invalid_Report",
        description = "Duplicates, cleanup suggestions, malformed/empty subflags, and Other category entries"
    ),
    MULTI_FILE_MATRIX_REPORT(
        title = "Multi-File Matrix Report",
        defaultFileName = "Multi_File_Matrix_Report",
        description = "Cross-file comparison matrix for 3+ mappings (unique, shared, and divergent codes)"
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
