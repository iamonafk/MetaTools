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

    fun getLabel(): String = when (this) {
        is BuiltIn -> displayName
        is ActiveCustom -> displayName
        is DeviceFile -> displayName
    }
}

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

data class ComparisonSummary(
    val oldSourceName: String,
    val newSourceName: String,
    val totalOldFlags: Int,
    val totalNewFlags: Int,
    val newFlagsCount: Int,
    val removedFlagsCount: Int,
    val updatedFlagsCount: Int,
    val unchangedFlagsCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class ComparisonReport(
    val summary: ComparisonSummary,
    val newFlags: List<NewFlagItem>,
    val newSubFlags: List<NewSubFlagItem>,
    val removedFlags: List<RemovedFlagItem>,
    val removedSubFlags: List<RemovedSubFlagItem>,
    val updatedFlagCodes: List<UpdatedFlagCodeItem>,
    val updatedSubFlags: List<UpdatedSubFlagItem>
) {
    val totalNewCount: Int get() = newFlags.size + newSubFlags.size
    val totalRemovedCount: Int get() = removedFlags.size + removedSubFlags.size
    val totalUpdatedCount: Int get() = updatedFlagCodes.size + updatedSubFlags.size
}

enum class CompareFilterTab {
    ALL,
    NEW,
    REMOVED,
    UPDATED
}
