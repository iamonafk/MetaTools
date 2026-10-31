package com.example.model

/**
 * Represents an individual sub-flag entry associated with a universal flag.
 *
 * Example: code = "1", name = "profile_toolbar_entrypoint_enabled"
 */
data class SubFlag(
    val code: String,
    val name: String,
    val parentFlagCode: String,
    val parentFlagName: String,
    val category: String
)

/**
 * Represents a Universal Flag (MetaConfig entry).
 *
 * Example: code = "130058", name = "ig_loot_android", category = "Direct"
 */
data class UniversalFlag(
    val code: String,
    val name: String,
    val subFlags: List<SubFlag>,
    val category: String,
    val originalEntry: String
)

/**
 * Represents a category of flags with dynamic counts.
 */
data class FlagCategory(
    val id: String,
    val name: String,
    val flagCount: Int,
    val isAllFlagsSpecial: Boolean = false
)

/**
 * Search result item representing either a matching Universal Flag or a matching Sub-Flag.
 */
data class FlagSearchResult(
    val universalFlag: UniversalFlag,
    val matchedSubFlag: SubFlag? = null,
    val matchType: MatchType = MatchType.UNIVERSAL_NAME,
    val score: Int = 0
)

enum class MatchType {
    UNIVERSAL_NAME,
    UNIVERSAL_CODE,
    SUBFLAG_NAME,
    SUBFLAG_CODE,
    CATEGORY
}

/**
 * Info about the currently active mapping file.
 */
data class MappingSource(
    val name: String,
    val isCustom: Boolean,
    val totalFlags: Int,
    val totalSubFlags: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)
