package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.FlagCategory
import com.example.model.FlagSearchResult
import com.example.model.MappingSource
import com.example.model.MatchType
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import com.squareup.moshi.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.File
import java.io.InputStream
import java.util.Locale

class MetaConfigRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("metatools_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USE_CUSTOM = "key_use_custom_mapping"
        private const val KEY_CUSTOM_FILENAME = "key_custom_filename"
        private const val CUSTOM_FILE_NAME = "custom_mapping.json"
        private const val BUILTIN_FILE_NAME = "Askxtylishh.json"
    }

    private var cachedFlags: List<UniversalFlag> = emptyList()
    private var flagsByCategory: Map<String, List<UniversalFlag>> = emptyMap()
    private var searchIndex: List<SearchIndexEntry> = emptyList()
    private var currentSourceInfo: MappingSource? = null

    private data class SearchIndexEntry(
        val flag: UniversalFlag,
        val normUniversalName: String,
        val universalNameTokens: List<String>,
        val universalCode: String,
        val categoryNorm: String,
        val subFlagEntries: List<SubFlagIndexEntry>
    )

    private data class SubFlagIndexEntry(
        val subFlag: SubFlag,
        val normName: String,
        val nameTokens: List<String>,
        val code: String
    )

    suspend fun getActiveMappingSource(): MappingSource {
        if (currentSourceInfo == null) {
            loadActiveMapping()
        }
        return currentSourceInfo!!
    }

    suspend fun getAllFlags(): List<UniversalFlag> {
        if (cachedFlags.isEmpty()) {
            loadActiveMapping()
        }
        return cachedFlags
    }

    suspend fun getCategories(): List<FlagCategory> {
        if (cachedFlags.isEmpty()) {
            loadActiveMapping()
        }
        val totalCount = cachedFlags.size

        return MetaConfigParser.CATEGORY_NAMES.map { categoryName ->
            if (categoryName == "See All Flags") {
                FlagCategory(
                    id = categoryName,
                    name = "Show all overridden MetaConfigs",
                    flagCount = totalCount,
                    isAllFlagsSpecial = true
                )
            } else {
                val count = flagsByCategory[categoryName]?.size ?: 0
                FlagCategory(
                    id = categoryName,
                    name = categoryName,
                    flagCount = count,
                    isAllFlagsSpecial = false
                )
            }
        }
    }

    suspend fun getFlagsForCategory(categoryName: String): List<UniversalFlag> {
        if (cachedFlags.isEmpty()) {
            loadActiveMapping()
        }
        return if (categoryName == "See All Flags" || categoryName == "Show all overridden MetaConfigs") {
            cachedFlags
        } else {
            flagsByCategory[categoryName] ?: emptyList()
        }
    }

    suspend fun loadActiveMapping(): MappingResult = withContext(Dispatchers.IO) {
        val useCustom = prefs.getBoolean(KEY_USE_CUSTOM, false)
        val customFile = File(context.filesDir, CUSTOM_FILE_NAME)

        if (useCustom && customFile.exists()) {
            try {
                val customName = prefs.getString(KEY_CUSTOM_FILENAME, "Custom Mapping") ?: "Custom Mapping"
                return@withContext parseStream(customFile.inputStream(), customName, isCustom = true)
            } catch (e: Exception) {
                // Fallback to built-in if custom file reading fails
                prefs.edit().putBoolean(KEY_USE_CUSTOM, false).apply()
            }
        }

        // Default: load built-in Askxtylishh.json
        val assetStream = context.assets.open(BUILTIN_FILE_NAME)
        parseStream(assetStream, BUILTIN_FILE_NAME, isCustom = false)
    }

    suspend fun importCustomMapping(uri: Uri, displayName: String): MappingResult = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext MappingResult.Error("Could not open file stream from selected URI")

        // First read and copy to a temporary file
        val tempFile = File(context.cacheDir, "temp_import.json")
        try {
            tempFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }

            // Validate and parse from temp file
            val parsedFlags = parseFlagsFromStream(tempFile.inputStream())
            if (parsedFlags.isEmpty()) {
                tempFile.delete()
                return@withContext MappingResult.Error("File contains no valid MetaConfig mapping entries")
            }

            // Success: commit to custom_mapping.json
            val destinationFile = File(context.filesDir, CUSTOM_FILE_NAME)
            tempFile.copyTo(destinationFile, overwrite = true)
            tempFile.delete()

            prefs.edit()
                .putBoolean(KEY_USE_CUSTOM, true)
                .putString(KEY_CUSTOM_FILENAME, displayName)
                .apply()

            // Update in-memory state
            buildState(parsedFlags, displayName, isCustom = true)

            return@withContext MappingResult.Success(currentSourceInfo!!)
        } catch (e: Exception) {
            tempFile.delete()
            return@withContext MappingResult.Error("Failed to parse mapping file: ${e.localizedMessage ?: "Invalid JSON format"}")
        }
    }

    suspend fun restoreBuiltInMapping(): MappingResult = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_USE_CUSTOM, false).apply()
        val assetStream = context.assets.open(BUILTIN_FILE_NAME)
        parseStream(assetStream, BUILTIN_FILE_NAME, isCustom = false)
    }

    suspend fun hasCustomMappingAvailable(): Boolean = withContext(Dispatchers.IO) {
        File(context.filesDir, CUSTOM_FILE_NAME).exists()
    }

    suspend fun switchToCustomMapping(): MappingResult = withContext(Dispatchers.IO) {
        val customFile = File(context.filesDir, CUSTOM_FILE_NAME)
        if (!customFile.exists()) {
            return@withContext MappingResult.Error("No custom mapping file has been imported yet")
        }
        val customName = prefs.getString(KEY_CUSTOM_FILENAME, "Custom Mapping") ?: "Custom Mapping"
        prefs.edit().putBoolean(KEY_USE_CUSTOM, true).apply()
        parseStream(customFile.inputStream(), customName, isCustom = true)
    }

    suspend fun getAvailableCompareSources(): List<com.example.model.CompareSource> = withContext(Dispatchers.IO) {
        val list = mutableListOf<com.example.model.CompareSource>(com.example.model.CompareSource.BuiltIn)
        val customFile = File(context.filesDir, CUSTOM_FILE_NAME)
        if (customFile.exists()) {
            val name = prefs.getString(KEY_CUSTOM_FILENAME, "Custom Mapping") ?: "Custom Mapping"
            list.add(com.example.model.CompareSource.ActiveCustom(name))
        }
        list
    }

    suspend fun loadFlagsFromSource(source: com.example.model.CompareSource): List<UniversalFlag> = withContext(Dispatchers.IO) {
        when (source) {
            is com.example.model.CompareSource.BuiltIn -> {
                val assetStream = context.assets.open(BUILTIN_FILE_NAME)
                parseFlagsFromStream(assetStream)
            }
            is com.example.model.CompareSource.ActiveCustom -> {
                val customFile = File(context.filesDir, CUSTOM_FILE_NAME)
                if (customFile.exists()) {
                    parseFlagsFromStream(customFile.inputStream())
                } else {
                    val assetStream = context.assets.open(BUILTIN_FILE_NAME)
                    parseFlagsFromStream(assetStream)
                }
            }
            is com.example.model.CompareSource.DeviceFile -> {
                val stream = context.contentResolver.openInputStream(source.uri)
                    ?: throw IllegalArgumentException("Cannot open stream for ${source.name}")
                parseFlagsFromStream(stream)
            }
        }
    }

    suspend fun compareMappings(
        oldSource: com.example.model.CompareSource,
        newSource: com.example.model.CompareSource
    ): com.example.model.ComparisonReport = withContext(Dispatchers.Default) {
        val oldFlags = loadFlagsFromSource(oldSource)
        val newFlags = loadFlagsFromSource(newSource)
        MappingCompareEngine.compare(
            oldFlags = oldFlags,
            newFlags = newFlags,
            oldSourceName = oldSource.getLabel(),
            newSourceName = newSource.getLabel()
        )
    }

    private fun parseStream(inputStream: InputStream, sourceName: String, isCustom: Boolean): MappingResult {
        return try {
            val flags = parseFlagsFromStream(inputStream)
            buildState(flags, sourceName, isCustom)
            MappingResult.Success(currentSourceInfo!!)
        } catch (e: Exception) {
            MappingResult.Error("Error parsing $sourceName: ${e.message}")
        }
    }

    private fun parseFlagsFromStream(inputStream: InputStream): List<UniversalFlag> {
        val flags = ArrayList<UniversalFlag>(6000)
        inputStream.source().buffer().use { bufferedSource ->
            val jsonReader = JsonReader.of(bufferedSource)
            jsonReader.beginArray()
            while (jsonReader.hasNext()) {
                val entryString = jsonReader.nextString()
                val parsed = MetaConfigParser.parseEntry(entryString)
                if (parsed != null) {
                    flags.add(parsed)
                }
            }
            jsonReader.endArray()
        }
        return flags
    }

    private fun buildState(flags: List<UniversalFlag>, sourceName: String, isCustom: Boolean) {
        cachedFlags = flags
        flagsByCategory = flags.groupBy { it.category }

        var totalSubFlags = 0
        val indexList = ArrayList<SearchIndexEntry>(flags.size)

        for (flag in flags) {
            totalSubFlags += flag.subFlags.size
            val normFlagName = normalizeToken(flag.name)
            val flagTokens = tokenize(flag.name)

            val subEntries = flag.subFlags.map { sf ->
                SubFlagIndexEntry(
                    subFlag = sf,
                    normName = normalizeToken(sf.name),
                    nameTokens = tokenize(sf.name),
                    code = sf.code
                )
            }

            indexList.add(
                SearchIndexEntry(
                    flag = flag,
                    normUniversalName = normFlagName,
                    universalNameTokens = flagTokens,
                    universalCode = flag.code,
                    categoryNorm = normalizeToken(flag.category),
                    subFlagEntries = subEntries
                )
            )
        }

        searchIndex = indexList
        currentSourceInfo = MappingSource(
            name = sourceName,
            isCustom = isCustom,
            totalFlags = flags.size,
            totalSubFlags = totalSubFlags
        )
    }

    /**
     * Performs intelligent multi-token search across universal flags, codes, sub-flags, and categories.
     */
    suspend fun search(query: String, maxResults: Int = 150): List<FlagSearchResult> = withContext(Dispatchers.Default) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        if (searchIndex.isEmpty()) {
            loadActiveMapping()
        }

        val queryLower = trimmed.lowercase(Locale.ROOT)
        val queryTokens = tokenize(queryLower)
        val isNumericQuery = queryLower.all { it.isDigit() }

        val results = ArrayList<FlagSearchResult>()
        val seenKeys = HashSet<String>()

        for (entry in searchIndex) {
            // 1. Exact universal code match
            if (entry.universalCode == queryLower) {
                val key = "u_${entry.flag.code}"
                if (seenKeys.add(key)) {
                    results.add(
                        FlagSearchResult(
                            universalFlag = entry.flag,
                            matchType = MatchType.UNIVERSAL_CODE,
                            score = 1000
                        )
                    )
                }
            }

            // 2. Exact universal name match
            if (entry.flag.name.equals(queryLower, ignoreCase = true)) {
                val key = "u_${entry.flag.code}"
                if (seenKeys.add(key)) {
                    results.add(
                        FlagSearchResult(
                            universalFlag = entry.flag,
                            matchType = MatchType.UNIVERSAL_NAME,
                            score = 900
                        )
                    )
                }
            }

            // 3. Multi-token match in universal flag name
            if (queryTokens.isNotEmpty() && queryTokens.all { token -> entry.normUniversalName.contains(token) }) {
                val key = "u_${entry.flag.code}"
                if (seenKeys.add(key)) {
                    val score = if (entry.normUniversalName.startsWith(queryTokens.first())) 800 else 700
                    results.add(
                        FlagSearchResult(
                            universalFlag = entry.flag,
                            matchType = MatchType.UNIVERSAL_NAME,
                            score = score
                        )
                    )
                }
            }

            // 4. Code partial match for numbers
            if (isNumericQuery && entry.universalCode.contains(queryLower)) {
                val key = "u_${entry.flag.code}"
                if (seenKeys.add(key)) {
                    results.add(
                        FlagSearchResult(
                            universalFlag = entry.flag,
                            matchType = MatchType.UNIVERSAL_CODE,
                            score = 650
                        )
                    )
                }
            }

            // 5. Search in sub-flags
            for (sub in entry.subFlagEntries) {
                // Exact sub-flag code match
                if (sub.code == queryLower) {
                    val key = "sub_${entry.flag.code}_${sub.subFlag.code}_${sub.subFlag.name}"
                    if (seenKeys.add(key)) {
                        results.add(
                            FlagSearchResult(
                                universalFlag = entry.flag,
                                matchedSubFlag = sub.subFlag,
                                matchType = MatchType.SUBFLAG_CODE,
                                score = 600
                            )
                        )
                    }
                }

                // Sub-flag name matches all query tokens
                if (queryTokens.isNotEmpty() && queryTokens.all { token -> sub.normName.contains(token) }) {
                    val key = "sub_${entry.flag.code}_${sub.subFlag.code}_${sub.subFlag.name}"
                    if (seenKeys.add(key)) {
                        val score = if (sub.subFlag.name.equals(queryLower, ignoreCase = true)) 750 else 550
                        results.add(
                            FlagSearchResult(
                                universalFlag = entry.flag,
                                matchedSubFlag = sub.subFlag,
                                matchType = MatchType.SUBFLAG_NAME,
                                score = score
                            )
                        )
                    }
                }
            }

            // 6. Category match
            if (entry.categoryNorm.contains(queryLower)) {
                val key = "u_${entry.flag.code}"
                if (seenKeys.add(key)) {
                    results.add(
                        FlagSearchResult(
                            universalFlag = entry.flag,
                            matchType = MatchType.CATEGORY,
                            score = 400
                        )
                    )
                }
            }

            if (results.size >= maxResults * 2) {
                break
            }
        }

        // Sort by score descending and take up to maxResults
        results.sortByDescending { it.score }
        results.take(maxResults)
    }

    private fun normalizeToken(text: String): String {
        return text.lowercase(Locale.ROOT).replace('_', ' ').replace('-', ' ')
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase(Locale.ROOT)
            .split(' ', '_', '-', ':', '.')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

sealed class MappingResult {
    data class Success(val source: MappingSource) : MappingResult()
    data class Error(val message: String) : MappingResult()
}
