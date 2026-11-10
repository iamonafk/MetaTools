package com.example.model

import java.util.UUID

/**
 * Type of mapping source used for code-to-name conversion.
 */
enum class CodeToNameMappingSource {
    BUILT_IN,
    CUSTOM
}

/**
 * JSON formatting style for output generation.
 */
enum class JsonFormatStyle {
    PRETTY,
    COMPACT
}

/**
 * Represents an individual subflag converted entry.
 */
data class ConvertedSubFlag(
    val numericCode: String,
    val resolvedName: String?,
    val value: String,
    val originalEntryString: String,
    val formattedOutputString: String,
    val isMapped: Boolean
)

/**
 * Represents a universal flag converted entry.
 */
data class ConvertedUniversalFlag(
    val numericCode: String,
    val resolvedName: String?,
    val originalKey: String,
    val formattedKey: String,
    val subFlags: List<ConvertedSubFlag>,
    val isMapped: Boolean,
    val isDuplicate: Boolean = false
) {
    val totalSubFlags: Int get() = subFlags.size
    val mappedSubFlags: Int get() = subFlags.count { it.isMapped }
    val unmappedSubFlags: Int get() = subFlags.count { !it.isMapped }
}

/**
 * An unmapped code item for reporting.
 */
data class UnmappedCodeItem(
    val universalCode: String,
    val universalName: String?,
    val subFlagCode: String?,
    val rawEntry: String,
    val isUniversalUnmapped: Boolean
)

/**
 * Comprehensive statistics for the conversion analysis dashboard.
 */
data class ConversionAnalysisStats(
    val totalUniversalFlags: Int = 0,
    val totalSubFlags: Int = 0,
    val mappedUniversalFlags: Int = 0,
    val mappedSubFlags: Int = 0,
    val unmappedUniversalCodes: Int = 0,
    val unmappedSubflagCodes: Int = 0,
    val totalConvertedEntries: Int = 0,
    val conversionErrors: Int = 0,
    val duplicateUniversalCount: Int = 0,
    val duplicateSubFlagCount: Int = 0,
    val mappingCoveragePercentage: Float = 0f
)

/**
 * Conversion history item saved locally.
 */
data class ConversionHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val sourceFileName: String,
    val mappingSourceName: String,
    val totalUniversal: Int,
    val totalSubFlags: Int,
    val coveragePercentage: Float
)

/**
 * Complete result of a code-to-name conversion.
 */
data class CodeToNameResult(
    val convertedJsonPretty: String,
    val convertedJsonCompact: String,
    val stats: ConversionAnalysisStats,
    val convertedFlags: List<ConvertedUniversalFlag>,
    val unmappedItems: List<UnmappedCodeItem>,
    val preservedRawEntries: Map<String, Any?>,
    val errorMessages: List<String> = emptyList()
)
