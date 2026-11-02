package com.example.model

/**
 * Metadata info structure inside an Instagram iBackup file.
 */
data class IBackupInfo(
    val id: Long,
    val releaseId: String,
    val name: String,
    val author: String,
    val authorId: Long,
    val version: String,
    val releaseDate: String,
    val changelog: String
)

/**
 * Conversion direction mode.
 */
enum class ConversionMode(val displayName: String, val description: String) {
    IBACKUP_TO_JSON(
        displayName = "iBackup to JSON",
        description = "Convert a complete Instagram iBackup file into a normal JSON backup file"
    ),
    JSON_TO_IBACKUP(
        displayName = "JSON to iBackup",
        description = "Convert a normal JSON backup file into a complete iBackup file with metadata"
    )
}

/**
 * Summary metrics of the converted / parsed backup content.
 */
data class ConversionPreview(
    val backupName: String,
    val totalUniversalFlags: Int,
    val totalSubFlagEntries: Int,
    val totalBackupEntries: Int,
    val mode: ConversionMode,
    val releaseVersion: String = "",
    val author: String = "",
    val releaseDate: String = "",
    val changelog: String = ""
)

/**
 * State of the converter session.
 */
data class ConverterState(
    val mode: ConversionMode = ConversionMode.IBACKUP_TO_JSON,
    val sourceFileName: String? = null,
    val rawInputContent: String = "",
    val convertedOutput: String? = null,
    val preview: ConversionPreview? = null,
    val suggestedFileName: String = "backup.json",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    // Metadata input fields for JSON to iBackup
    val inputBackupName: String = "",
    val inputAuthor: String = "",
    val inputVersion: String = "1",
    val inputChangelog: String = "",
    val inputBackupId: String = "",
    val inputAuthorId: String = ""
)
