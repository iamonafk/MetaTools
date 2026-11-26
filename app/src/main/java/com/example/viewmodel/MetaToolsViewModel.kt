package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CompareSearchIndex
import com.example.data.FlagAnalysisEngine
import com.example.data.MappingCompareEngine
import com.example.data.MappingDiagnosticsParser
import com.example.data.MappingIntelligenceAnalyzer
import com.example.data.MappingResult
import com.example.data.MetaConfigRepository
import com.example.model.CatalogDisplayMode
import com.example.model.CatalogFlagItem
import com.example.model.CatalogScopeFilter
import com.example.model.CatalogSortOption
import com.example.model.CompareFilterTab
import com.example.model.CompareHistoryRecord
import com.example.model.CompareMode
import com.example.model.CompareQuickFilter
import com.example.model.CompareResultSection
import com.example.model.CompareSectionTab
import com.example.model.CompareSource
import com.example.model.CompareSourceSlot
import com.example.model.ComparisonReport
import com.example.model.ExportFileFormat
import com.example.model.ExportReportType
import com.example.model.FlagAnalysisFilter
import com.example.model.FlagCategory
import com.example.model.FlagSearchResult
import com.example.model.MappingSource
import com.example.model.MultiFileComparisonReport
import com.example.model.ParsedBackupState
import com.example.model.SessionDiffReport
import com.example.model.SourcePreviewInfo
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

enum class AppTab {
    HOME,
    FLAGS,
    TOOLS,
    PROFILE
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShareText(val text: String, val title: String) : UiEvent()
}

@OptIn(FlowPreview::class)
class MetaToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MetaConfigRepository(application)
    private val prefs = application.getSharedPreferences("metatools_prefs", Context.MODE_PRIVATE)

    private val _currentTab = MutableStateFlow(AppTab.FLAGS)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _mappingSource = MutableStateFlow<MappingSource?>(null)
    val mappingSource: StateFlow<MappingSource?> = _mappingSource.asStateFlow()

    private val _categories = MutableStateFlow<List<FlagCategory>>(emptyList())
    val categories: StateFlow<List<FlagCategory>> = _categories.asStateFlow()

    private val _hasCustomMapping = MutableStateFlow(false)
    val hasCustomMapping: StateFlow<Boolean> = _hasCustomMapping.asStateFlow()

    // Expanded category ID (null if none expanded, or set of expanded category IDs)
    private val _expandedCategories = MutableStateFlow<Set<String>>(emptySet())
    val expandedCategories: StateFlow<Set<String>> = _expandedCategories.asStateFlow()

    // Expanded universal flags IDs (for sub-flag disclosure)
    private val _expandedFlags = MutableStateFlow<Set<String>>(emptySet())
    val expandedFlags: StateFlow<Set<String>> = _expandedFlags.asStateFlow()

    // Cache of category flags loaded for expanded categories
    private val _categoryFlagsMap = MutableStateFlow<Map<String, List<UniversalFlag>>>(emptyMap())
    val categoryFlagsMap: StateFlow<Map<String, List<UniversalFlag>>> = _categoryFlagsMap.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FlagSearchResult>>(emptyList())
    val searchResults: StateFlow<List<FlagSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    // Settings sheet visibility
    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    // Mapping source selector dialog
    private val _showSourceDialog = MutableStateFlow(false)
    val showSourceDialog: StateFlow<Boolean> = _showSourceDialog.asStateFlow()

    // Mapping comparison state
    private val _availableCompareSources = MutableStateFlow<List<CompareSource>>(listOf(CompareSource.BuiltIn))
    val availableCompareSources: StateFlow<List<CompareSource>> = _availableCompareSources.asStateFlow()

    private val _oldCompareSource = MutableStateFlow<CompareSource>(CompareSource.BuiltIn)
    val oldCompareSource: StateFlow<CompareSource> = _oldCompareSource.asStateFlow()

    private val _newCompareSource = MutableStateFlow<CompareSource?>(null)
    val newCompareSource: StateFlow<CompareSource?> = _newCompareSource.asStateFlow()

    private val _oldSourcePreview = MutableStateFlow<SourcePreviewInfo?>(null)
    val oldSourcePreview: StateFlow<SourcePreviewInfo?> = _oldSourcePreview.asStateFlow()

    private val _newSourcePreview = MutableStateFlow<SourcePreviewInfo?>(null)
    val newSourcePreview: StateFlow<SourcePreviewInfo?> = _newSourcePreview.asStateFlow()

    private val _isComparing = MutableStateFlow(false)
    val isComparing: StateFlow<Boolean> = _isComparing.asStateFlow()

    private val _comparisonReport = MutableStateFlow<ComparisonReport?>(null)
    val comparisonReport: StateFlow<ComparisonReport?> = _comparisonReport.asStateFlow()

    private val _compareSectionTab = MutableStateFlow(CompareSectionTab.CHANGES)
    val compareSectionTab: StateFlow<CompareSectionTab> = _compareSectionTab.asStateFlow()

    private val _compareFilterTab = MutableStateFlow(CompareFilterTab.ALL)
    val compareFilterTab: StateFlow<CompareFilterTab> = _compareFilterTab.asStateFlow()

    private val _compareQuickFilter = MutableStateFlow(CompareQuickFilter.ALL)
    val compareQuickFilter: StateFlow<CompareQuickFilter> = _compareQuickFilter.asStateFlow()

    private val _compareSearchQuery = MutableStateFlow("")
    val compareSearchQuery: StateFlow<String> = _compareSearchQuery.asStateFlow()

    private val _catalogCategoryFilter = MutableStateFlow("See All Flags")
    val catalogCategoryFilter: StateFlow<String> = _catalogCategoryFilter.asStateFlow()

    private val _catalogSortOption = MutableStateFlow(CatalogSortOption.ALPHABETICAL)
    val catalogSortOption: StateFlow<CatalogSortOption> = _catalogSortOption.asStateFlow()

    private val _catalogScopeFilter = MutableStateFlow(CatalogScopeFilter.ALL)
    val catalogScopeFilter: StateFlow<CatalogScopeFilter> = _catalogScopeFilter.asStateFlow()

    private val _selectedCatalogFlagDetail = MutableStateFlow<CatalogFlagItem?>(null)
    val selectedCatalogFlagDetail: StateFlow<CatalogFlagItem?> = _selectedCatalogFlagDetail.asStateFlow()

    private val _compareHistory = MutableStateFlow<List<CompareHistoryRecord>>(emptyList())
    val compareHistory: StateFlow<List<CompareHistoryRecord>> = _compareHistory.asStateFlow()

    // Advanced Compare & Analyzer state (3+/Multi-file, Session Diff, 15 Sections, Filters, Productivity)
    private val _compareMode = MutableStateFlow(CompareMode.TWO_FILE)
    val compareMode: StateFlow<CompareMode> = _compareMode.asStateFlow()

    private val _multiCompareSlots = MutableStateFlow(
        listOf(
            CompareSourceSlot(id = "slot_1", label = "Mapping #1 (Baseline)", source = CompareSource.BuiltIn),
            CompareSourceSlot(id = "slot_2", label = "Mapping #2"),
            CompareSourceSlot(id = "slot_3", label = "Mapping #3")
        )
    )
    val multiCompareSlots: StateFlow<List<CompareSourceSlot>> = _multiCompareSlots.asStateFlow()

    private val _multiFileReport = MutableStateFlow<MultiFileComparisonReport?>(null)
    val multiFileReport: StateFlow<MultiFileComparisonReport?> = _multiFileReport.asStateFlow()

    private val _sessionDiffReport = MutableStateFlow<SessionDiffReport?>(null)
    val sessionDiffReport: StateFlow<SessionDiffReport?> = _sessionDiffReport.asStateFlow()

    private val _activeResultSection = MutableStateFlow(CompareResultSection.OVERVIEW)
    val activeResultSection: StateFlow<CompareResultSection> = _activeResultSection.asStateFlow()

    private val _selectedCatalogCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCatalogCategories: StateFlow<Set<String>> = _selectedCatalogCategories.asStateFlow()

    private val _namespaceFilter = MutableStateFlow("All")
    val namespaceFilter: StateFlow<String> = _namespaceFilter.asStateFlow()

    private val _exactSubflagCountFilter = MutableStateFlow<Int?>(null)
    val exactSubflagCountFilter: StateFlow<Int?> = _exactSubflagCountFilter.asStateFlow()

    private val _onlyInOneMappingFilter = MutableStateFlow(false)
    val onlyInOneMappingFilter: StateFlow<Boolean> = _onlyInOneMappingFilter.asStateFlow()

    private val _favoritesOnlyFilter = MutableStateFlow(false)
    val favoritesOnlyFilter: StateFlow<Boolean> = _favoritesOnlyFilter.asStateFlow()

    private val _favoriteFlagNames = MutableStateFlow<Set<String>>(emptySet())
    val favoriteFlagNames: StateFlow<Set<String>> = _favoriteFlagNames.asStateFlow()

    private val _catalogDisplayMode = MutableStateFlow(CatalogDisplayMode.EXPANDED)
    val catalogDisplayMode: StateFlow<CatalogDisplayMode> = _catalogDisplayMode.asStateFlow()

    private val _expandAllCatalogCategories = MutableStateFlow(false)
    val expandAllCatalogCategories: StateFlow<Boolean> = _expandAllCatalogCategories.asStateFlow()

    private val _selectedResultKeys = MutableStateFlow<Set<String>>(emptySet())
    val selectedResultKeys: StateFlow<Set<String>> = _selectedResultKeys.asStateFlow()

    private val _compareSearchHistory = MutableStateFlow<List<String>>(emptyList())
    val compareSearchHistory: StateFlow<List<String>> = _compareSearchHistory.asStateFlow()

    private val _compareSearchIndex = MutableStateFlow<CompareSearchIndex?>(null)
    val compareSearchIndex: StateFlow<CompareSearchIndex?> = _compareSearchIndex.asStateFlow()

    init {
        loadRecentSearches()
        _favoriteFlagNames.value = repository.getFavoriteFlagNames()
        _compareSearchHistory.value = repository.getCompareSearchHistory()
        loadInitialData()
        previewSourceAsync(isOld = true, source = CompareSource.BuiltIn)
        previewMultiSlotAsync("slot_1", CompareSource.BuiltIn)
        loadCompareHistoryAndLastSession()

        // Debounced search trigger
        viewModelScope.launch {
            _searchQuery
                .debounce(200)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.trim().isEmpty()) {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                    } else {
                        _isSearching.value = true
                        val results = repository.search(query)
                        _searchResults.value = results
                        _isSearching.value = false
                    }
                }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun openSettings() {
        _showSettingsSheet.value = true
    }

    fun closeSettings() {
        _showSettingsSheet.value = false
    }

    fun openSourceDialog() {
        _showSourceDialog.value = true
    }

    fun closeSourceDialog() {
        _showSourceDialog.value = false
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun executeSearch(query: String) {
        _searchQuery.value = query
        saveRecentSearch(query)
    }

    fun toggleCategory(categoryId: String) {
        val current = _expandedCategories.value.toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
            // Load flags for this category if not yet cached
            if (!_categoryFlagsMap.value.containsKey(categoryId)) {
                viewModelScope.launch {
                    val flags = repository.getFlagsForCategory(categoryId)
                    val map = _categoryFlagsMap.value.toMutableMap()
                    map[categoryId] = flags
                    _categoryFlagsMap.value = map
                }
            }
        }
        _expandedCategories.value = current
    }

    fun toggleFlagExpansion(flagCode: String) {
        val current = _expandedFlags.value.toMutableSet()
        if (current.contains(flagCode)) {
            current.remove(flagCode)
        } else {
            current.add(flagCode)
        }
        _expandedFlags.value = current
    }

    /**
     * Copy actions strictly copy the exact raw value with zero formatting.
     */
    fun copyUniversalFlagName(flag: UniversalFlag) {
        copyToClipboard(flag.name, "Copied universal flag name: ${flag.name}")
    }

    fun copyUniversalFlagCode(flag: UniversalFlag) {
        copyToClipboard(flag.code, "Copied universal code: ${flag.code}")
    }

    fun copySubFlagName(subFlag: SubFlag) {
        copyToClipboard(subFlag.name, "Copied sub-flag name: ${subFlag.name}")
    }

    fun copySubFlagCode(subFlag: SubFlag) {
        copyToClipboard(subFlag.code, "Copied sub-flag code: ${subFlag.code}")
    }

    private fun copyToClipboard(text: String, message: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("MetaConfig", text)
        clipboard.setPrimaryClip(clip)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Copied to clipboard"))
        }
    }

    fun shareUniversalFlag(flag: UniversalFlag) {
        val shareText = "${flag.name} (${flag.code})"
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShareText(shareText, "Share Universal Flag"))
        }
    }

    fun shareSubFlag(subFlag: SubFlag) {
        val shareText = "${subFlag.name} (${subFlag.code})"
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShareText(shareText, "Share Sub-Flag"))
        }
    }

    fun shareCombinedFlag(flag: UniversalFlag) {
        val builder = StringBuilder()
        builder.append(flag.code).append(":").append(flag.name)
        for (sub in flag.subFlags) {
            builder.append(":").append(sub.code).append(":").append(sub.name)
        }
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShareText(builder.toString(), "Share MetaConfig Entry"))
        }
    }

    fun importCustomMapping(uri: Uri, displayName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.importCustomMapping(uri, displayName)) {
                is MappingResult.Success -> {
                    refreshData(result.source)
                    _uiEvents.emit(UiEvent.ShowToast("Imported mapping: ${result.source.name} (${result.source.totalFlags} flags)"))
                }
                is MappingResult.Error -> {
                    _isLoading.value = false
                    _uiEvents.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun restoreBuiltInMapping() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.restoreBuiltInMapping()) {
                is MappingResult.Success -> {
                    refreshData(result.source)
                    _uiEvents.emit(UiEvent.ShowToast("Restored built-in mapping Askxtylishh.json"))
                }
                is MappingResult.Error -> {
                    _isLoading.value = false
                    _uiEvents.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun switchToCustomMapping() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.switchToCustomMapping()) {
                is MappingResult.Success -> {
                    refreshData(result.source)
                    _uiEvents.emit(UiEvent.ShowToast("Switched to custom mapping"))
                }
                is MappingResult.Error -> {
                    _isLoading.value = false
                    _uiEvents.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            val source = repository.getActiveMappingSource()
            _hasCustomMapping.value = repository.hasCustomMappingAvailable()
            refreshData(source)
        }
    }

    private suspend fun refreshData(source: MappingSource) {
        _mappingSource.value = source
        _categories.value = repository.getCategories()
        _hasCustomMapping.value = repository.hasCustomMappingAvailable()
        _categoryFlagsMap.value = emptyMap()
        _expandedCategories.value = emptySet()
        _expandedFlags.value = emptySet()
        _availableCompareSources.value = repository.getAvailableCompareSources()
        _isLoading.value = false
    }

    private fun loadRecentSearches() {
        val saved = prefs.getStringSet("recent_searches", emptySet()) ?: emptySet()
        _recentSearches.value = saved.toList().take(10)
    }

    private fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val current = _recentSearches.value.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val updated = current.take(10)
        _recentSearches.value = updated
        prefs.edit().putStringSet("recent_searches", updated.toSet()).apply()
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit().remove("recent_searches").apply()
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Search history cleared"))
        }
    }

    // Comparison actions
    fun refreshAvailableCompareSources() {
        viewModelScope.launch {
            _availableCompareSources.value = repository.getAvailableCompareSources()
            _compareHistory.value = repository.loadCompareHistoryList()
            if (_oldSourcePreview.value == null) {
                previewSourceAsync(isOld = true, source = _oldCompareSource.value)
            }
        }
    }

    private fun loadCompareHistoryAndLastSession() {
        viewModelScope.launch {
            val history = repository.loadCompareHistoryList()
            _compareHistory.value = history
            if (_comparisonReport.value == null && history.isNotEmpty()) {
                val restored = repository.loadLastComparisonSnapshot()
                if (restored != null) {
                    _comparisonReport.value = restored
                    rebuildSearchIndexAsync(restored)
                }
            }
        }
    }

    private fun rebuildSearchIndexAsync(report: ComparisonReport) {
        viewModelScope.launch(Dispatchers.Default) {
            _compareSearchIndex.value = MappingIntelligenceAnalyzer.buildSearchIndex(report)
        }
    }

    private fun previewSourceAsync(isOld: Boolean, source: CompareSource?) {
        if (source == null) {
            if (isOld) _oldSourcePreview.value = null else _newSourcePreview.value = null
            return
        }
        viewModelScope.launch {
            try {
                val diag = repository.loadDiagnosticsFromSource(source)
                val info = MappingDiagnosticsParser.toSourcePreview(diag)
                if (isOld) _oldSourcePreview.value = info else _newSourcePreview.value = info
            } catch (e: Exception) {
                val errorInfo = SourcePreviewInfo(
                    sourceName = source.getLabel(),
                    isValid = false,
                    validationMessage = e.localizedMessage ?: "Failed to read source",
                    universalFlagCount = 0,
                    subFlagCount = 0,
                    byteSize = 0L,
                    duplicateCount = 0,
                    invalidCount = 0,
                    previewEntries = emptyList()
                )
                if (isOld) _oldSourcePreview.value = errorInfo else _newSourcePreview.value = errorInfo
            }
        }
    }

    fun setOldCompareSource(source: CompareSource) {
        _oldCompareSource.value = source
        previewSourceAsync(isOld = true, source = source)
    }

    fun setNewCompareSource(source: CompareSource) {
        _newCompareSource.value = source
        previewSourceAsync(isOld = false, source = source)
    }

    fun setOldCompareSourceFromUri(uri: Uri, displayName: String) {
        val src = CompareSource.DeviceFile(uri, displayName)
        _oldCompareSource.value = src
        previewSourceAsync(isOld = true, source = src)
    }

    fun setNewCompareSourceFromUri(uri: Uri, displayName: String) {
        val src = CompareSource.DeviceFile(uri, displayName)
        _newCompareSource.value = src
        previewSourceAsync(isOld = false, source = src)
    }

    fun setOldCompareSourceFromPaste(rawJson: String, name: String) {
        val label = name.trim().ifEmpty { "Old Pasted Mapping" }
        val src = CompareSource.PastedJson(rawJson, label)
        _oldCompareSource.value = src
        previewSourceAsync(isOld = true, source = src)
    }

    fun setNewCompareSourceFromPaste(rawJson: String, name: String) {
        val label = name.trim().ifEmpty { "New Pasted Mapping" }
        val src = CompareSource.PastedJson(rawJson, label)
        _newCompareSource.value = src
        previewSourceAsync(isOld = false, source = src)
    }

    fun clearNewCompareSource() {
        _newCompareSource.value = null
        _newSourcePreview.value = null
    }

    fun swapCompareSources() {
        val currentOld = _oldCompareSource.value
        val currentNew = _newCompareSource.value
        val currentOldPreview = _oldSourcePreview.value
        val currentNewPreview = _newSourcePreview.value
        if (currentNew != null) {
            _oldCompareSource.value = currentNew
            _newCompareSource.value = currentOld
            _oldSourcePreview.value = currentNewPreview
            _newSourcePreview.value = currentOldPreview
        }
    }

    fun runComparison() {
        val old = _oldCompareSource.value
        val new = _newCompareSource.value
        if (new == null) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("Please select a New Mapping file to compare"))
            }
            return
        }
        viewModelScope.launch {
            _isComparing.value = true
            try {
                val report = repository.compareMappings(old, new)
                _comparisonReport.value = report
                _multiFileReport.value = null
                _sessionDiffReport.value = null
                _selectedResultKeys.value = emptySet()
                _activeResultSection.value = CompareResultSection.OVERVIEW
                _compareSectionTab.value = CompareSectionTab.CHANGES
                _compareFilterTab.value = CompareFilterTab.ALL
                _compareQuickFilter.value = CompareQuickFilter.ALL
                rebuildSearchIndexAsync(report)
                repository.saveComparisonToHistory(report)
                _compareHistory.value = repository.loadCompareHistoryList()
                _uiEvents.emit(
                    UiEvent.ShowToast(
                        "Comparison complete: +${report.totalNewCount} new, -${report.totalRemovedCount} removed, ~${report.totalUpdatedCount} updated"
                    )
                )
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Comparison failed: ${e.message}"))
            } finally {
                _isComparing.value = false
            }
        }
    }

    // 3-File & Multi-File Comparison Management
    fun setCompareMode(mode: CompareMode) {
        _compareMode.value = mode
    }

    fun addMultiCompareSlot() {
        val list = _multiCompareSlots.value
        if (list.size >= 8) return
        val nextNum = list.size + 1
        _multiCompareSlots.value = list + CompareSourceSlot(
            id = "slot_${System.currentTimeMillis()}_$nextNum",
            label = "Mapping #$nextNum"
        )
    }

    fun removeMultiCompareSlot(slotId: String) {
        val list = _multiCompareSlots.value
        if (list.size <= 2) return
        _multiCompareSlots.value = list.filterNot { it.id == slotId }.mapIndexed { idx, slot ->
            val defaultPrefix = if (idx == 0) "Mapping #1 (Baseline)" else "Mapping #${idx + 1}"
            slot.copy(label = defaultPrefix)
        }
    }

    fun setMultiSlotSource(slotId: String, source: CompareSource) {
        _multiCompareSlots.value = _multiCompareSlots.value.map { slot ->
            if (slot.id == slotId) slot.copy(source = source) else slot
        }
        previewMultiSlotAsync(slotId, source)
    }

    fun setMultiSlotFromUri(slotId: String, uri: Uri, displayName: String) {
        setMultiSlotSource(slotId, CompareSource.DeviceFile(uri, displayName))
    }

    fun setMultiSlotFromPaste(slotId: String, rawJson: String, name: String) {
        val label = name.trim().ifEmpty { "Pasted Mapping" }
        setMultiSlotSource(slotId, CompareSource.PastedJson(rawJson, label))
    }

    private fun previewMultiSlotAsync(slotId: String, source: CompareSource?) {
        if (source == null) return
        viewModelScope.launch {
            val preview = try {
                val diag = repository.loadDiagnosticsFromSource(source)
                MappingDiagnosticsParser.toSourcePreview(diag)
            } catch (e: Exception) {
                SourcePreviewInfo(
                    sourceName = source.getLabel(),
                    isValid = false,
                    validationMessage = e.localizedMessage ?: "Failed to read source",
                    universalFlagCount = 0,
                    subFlagCount = 0,
                    byteSize = 0L,
                    duplicateCount = 0,
                    invalidCount = 0,
                    previewEntries = emptyList()
                )
            }
            _multiCompareSlots.value = _multiCompareSlots.value.map { slot ->
                if (slot.id == slotId) slot.copy(preview = preview) else slot
            }
        }
    }

    fun runMultiFileComparison() {
        val selectedSources = _multiCompareSlots.value.mapNotNull { it.source }
        if (selectedSources.size < 2) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("Select at least 2 mapping files in Multi-File mode"))
            }
            return
        }
        viewModelScope.launch {
            _isComparing.value = true
            try {
                val multiReport = repository.compareMultipleMappings(selectedSources)
                _multiFileReport.value = multiReport
                _sessionDiffReport.value = null
                _selectedResultKeys.value = emptySet()
                multiReport.baselineVsLatestReport?.let { baseRep ->
                    _comparisonReport.value = baseRep
                    rebuildSearchIndexAsync(baseRep)
                }
                _activeResultSection.value = CompareResultSection.OVERVIEW
                _uiEvents.emit(
                    UiEvent.ShowToast(
                        "Compared ${multiReport.sourceNames.size} mappings: ${multiReport.uniqueFlagsTotal} unique flags, ${multiReport.onlyInOneFileCount} single-file only"
                    )
                )
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Multi-file compare failed: ${e.message}"))
            } finally {
                _isComparing.value = false
            }
        }
    }

    fun compareTwoHistorySessions(recordA: CompareHistoryRecord, recordB: CompareHistoryRecord) {
        viewModelScope.launch {
            _isComparing.value = true
            try {
                val repA = repository.loadComparisonHistorySnapshot(recordA.id)
                val repB = repository.loadComparisonHistorySnapshot(recordB.id)
                if (repA == null || repB == null) {
                    _uiEvents.emit(UiEvent.ShowToast("One or both saved session snapshots are unavailable"))
                    return@launch
                }
                val diff = MappingIntelligenceAnalyzer.compareHistorySessions(
                    sessionALabel = "${recordA.oldSourceName} → ${recordA.newSourceName}",
                    reportA = repA,
                    sessionBLabel = "${recordB.oldSourceName} → ${recordB.newSourceName}",
                    reportB = repB
                )
                _sessionDiffReport.value = diff
                _comparisonReport.value = repB
                rebuildSearchIndexAsync(repB)
                _activeResultSection.value = CompareResultSection.OVERVIEW
                _uiEvents.emit(
                    UiEvent.ShowToast(
                        "Compared sessions: ${diff.newlyAppearedInB.size} newly appeared, ${diff.disappearedInB.size} disappeared"
                    )
                )
            } finally {
                _isComparing.value = false
            }
        }
    }

    fun clearComparison() {
        _comparisonReport.value = null
        _multiFileReport.value = null
        _sessionDiffReport.value = null
        _compareSearchIndex.value = null
        _compareSearchQuery.value = ""
        _compareFilterTab.value = CompareFilterTab.ALL
        _compareQuickFilter.value = CompareQuickFilter.ALL
        _compareSectionTab.value = CompareSectionTab.CHANGES
        _activeResultSection.value = CompareResultSection.OVERVIEW
        _catalogCategoryFilter.value = "See All Flags"
        _selectedCatalogCategories.value = emptySet()
        _namespaceFilter.value = "All"
        _exactSubflagCountFilter.value = null
        _onlyInOneMappingFilter.value = false
        _favoritesOnlyFilter.value = false
        _selectedResultKeys.value = emptySet()
    }

    fun refreshAndReindexComparison() {
        val currentReport = _comparisonReport.value ?: return
        viewModelScope.launch {
            _isComparing.value = true
            try {
                if (_compareMode.value == CompareMode.MULTI_FILE && _multiFileReport.value != null) {
                    val selectedSources = _multiCompareSlots.value.mapNotNull { it.source }
                    if (selectedSources.size >= 2) {
                        val multi = repository.compareMultipleMappings(selectedSources)
                        _multiFileReport.value = multi
                        multi.baselineVsLatestReport?.let {
                            _comparisonReport.value = it
                            rebuildSearchIndexAsync(it)
                        }
                        _uiEvents.emit(UiEvent.ShowToast("Re-indexed ${multi.sourceNames.size} mappings"))
                        return@launch
                    }
                }
                if (_newCompareSource.value != null) {
                    val refreshed = repository.compareMappings(_oldCompareSource.value, _newCompareSource.value!!)
                    _comparisonReport.value = refreshed
                    rebuildSearchIndexAsync(refreshed)
                    _uiEvents.emit(UiEvent.ShowToast("Re-indexed ${refreshed.catalogFlags.size} flags"))
                } else {
                    rebuildSearchIndexAsync(currentReport)
                    _uiEvents.emit(UiEvent.ShowToast("Rebuilt search index (${currentReport.catalogFlags.size} flags)"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Refresh failed: ${e.message}"))
            } finally {
                _isComparing.value = false
            }
        }
    }

    fun setActiveResultSection(section: CompareResultSection) {
        _activeResultSection.value = section
        when (section) {
            CompareResultSection.COMPLETE_CATALOG -> _compareSectionTab.value = CompareSectionTab.CATALOG
            CompareResultSection.NEW_FLAGS -> {
                _compareSectionTab.value = CompareSectionTab.CHANGES
                _compareFilterTab.value = CompareFilterTab.NEW
            }
            CompareResultSection.REMOVED_FLAGS -> {
                _compareSectionTab.value = CompareSectionTab.CHANGES
                _compareFilterTab.value = CompareFilterTab.REMOVED
            }
            CompareResultSection.UPDATED_FLAGS -> {
                _compareSectionTab.value = CompareSectionTab.CHANGES
                _compareFilterTab.value = CompareFilterTab.UPDATED
            }
            CompareResultSection.UNCHANGED_FLAGS -> {
                _compareSectionTab.value = CompareSectionTab.CHANGES
                _compareFilterTab.value = CompareFilterTab.UNCHANGED
            }
            CompareResultSection.CATEGORIES -> _compareSectionTab.value = CompareSectionTab.ANALYTICS
            CompareResultSection.MAPPING_HEALTH -> _compareSectionTab.value = CompareSectionTab.HEALTH
            else -> _compareSectionTab.value = CompareSectionTab.CHANGES
        }
    }

    fun setCompareSectionTab(tab: CompareSectionTab) {
        _compareSectionTab.value = tab
    }

    fun setCompareFilterTab(tab: CompareFilterTab) {
        _compareFilterTab.value = tab
        _compareQuickFilter.value = when (tab) {
            CompareFilterTab.ALL -> CompareQuickFilter.ALL
            CompareFilterTab.NEW -> CompareQuickFilter.NEW
            CompareFilterTab.REMOVED -> CompareQuickFilter.REMOVED
            CompareFilterTab.UPDATED -> CompareQuickFilter.UPDATED
            CompareFilterTab.UNCHANGED -> CompareQuickFilter.UNCHANGED
        }
    }

    fun setCompareQuickFilter(filter: CompareQuickFilter) {
        _compareQuickFilter.value = filter
        _compareFilterTab.value = when (filter) {
            CompareQuickFilter.NEW -> CompareFilterTab.NEW
            CompareQuickFilter.REMOVED -> CompareFilterTab.REMOVED
            CompareQuickFilter.UPDATED -> CompareFilterTab.UPDATED
            CompareQuickFilter.UNCHANGED -> CompareFilterTab.UNCHANGED
            else -> CompareFilterTab.ALL
        }
    }

    fun onCompareSearchQueryChanged(query: String) {
        _compareSearchQuery.value = query
    }

    fun commitCompareSearchToHistory(query: String = _compareSearchQuery.value) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            _compareSearchHistory.value = repository.saveCompareSearchQuery(trimmed)
        }
    }

    fun clearCompareSearch() {
        _compareSearchQuery.value = ""
    }

    fun clearCompareSearchHistory() {
        repository.clearCompareSearchHistory()
        _compareSearchHistory.value = emptyList()
    }

    fun setCatalogCategoryFilter(category: String) {
        _catalogCategoryFilter.value = category
        _selectedCatalogCategories.value = if (category == "See All Flags") emptySet() else setOf(category)
    }

    fun toggleCatalogCategorySelection(category: String) {
        if (category == "See All Flags") {
            _selectedCatalogCategories.value = emptySet()
            _catalogCategoryFilter.value = "See All Flags"
            return
        }
        val next = _selectedCatalogCategories.value.toMutableSet()
        if (!next.add(category)) {
            next.remove(category)
        }
        _selectedCatalogCategories.value = next
        _catalogCategoryFilter.value = when (next.size) {
            0 -> "See All Flags"
            1 -> next.first()
            else -> "${next.size} Categories"
        }
    }

    fun clearCatalogCategoryFilters() {
        _selectedCatalogCategories.value = emptySet()
        _catalogCategoryFilter.value = "See All Flags"
    }

    fun setNamespaceFilter(prefix: String) {
        _namespaceFilter.value = prefix
    }

    fun setExactSubflagCountFilter(count: Int?) {
        _exactSubflagCountFilter.value = count
    }

    fun setOnlyInOneMappingFilter(enabled: Boolean) {
        _onlyInOneMappingFilter.value = enabled
    }

    fun setFavoritesOnlyFilter(enabled: Boolean) {
        _favoritesOnlyFilter.value = enabled
    }

    fun toggleFavoriteFlag(flagName: String) {
        val updated = repository.toggleFavoriteFlag(flagName)
        _favoriteFlagNames.value = updated
        viewModelScope.launch {
            val isNowFav = updated.contains(flagName)
            _uiEvents.emit(UiEvent.ShowToast(if (isNowFav) "Pinned $flagName to Favorites" else "Removed $flagName from Favorites"))
        }
    }

    fun setCatalogDisplayMode(mode: CatalogDisplayMode) {
        _catalogDisplayMode.value = mode
    }

    fun toggleExpandAllCatalogCategories() {
        _expandAllCatalogCategories.value = !_expandAllCatalogCategories.value
    }

    fun toggleResultSelection(flagName: String) {
        val next = _selectedResultKeys.value.toMutableSet()
        if (!next.add(flagName)) {
            next.remove(flagName)
        }
        _selectedResultKeys.value = next
    }

    fun selectResultKeys(keys: Collection<String>) {
        _selectedResultKeys.value = _selectedResultKeys.value + keys
    }

    fun clearResultSelection() {
        _selectedResultKeys.value = emptySet()
    }

    fun bulkCopySelected(report: ComparisonReport) {
        val selected = _selectedResultKeys.value
        if (selected.isEmpty()) return
        val items = report.catalogFlags.filter { selected.contains(it.name) }
        val text = MappingIntelligenceAnalyzer.generateCatalogListExport(
            title = "Selected Flags (${items.size})",
            items = items,
            format = ExportFileFormat.TXT
        )
        copyToClipboard(text, "Copied ${items.size} selected flags")
    }

    fun copyAllFlagsInCategory(report: ComparisonReport, categoryName: String) {
        val items = report.catalogFlags.filter { it.category == categoryName }
        val text = MappingIntelligenceAnalyzer.generateCatalogListExport(
            title = "Category: $categoryName",
            items = items,
            format = ExportFileFormat.TXT
        )
        copyToClipboard(text, "Copied ${items.size} flags from $categoryName")
    }

    fun setCatalogSortOption(sort: CatalogSortOption) {
        _catalogSortOption.value = sort
    }

    fun setCatalogScopeFilter(scope: CatalogScopeFilter) {
        _catalogScopeFilter.value = scope
    }

    fun openFlagDetail(item: CatalogFlagItem) {
        _selectedCatalogFlagDetail.value = item
    }

    fun openFlagDetailByName(flagName: String) {
        val report = _comparisonReport.value ?: return
        val match = _compareSearchIndex.value?.byName?.get(flagName)
            ?: report.catalogFlags.firstOrNull { it.name == flagName }
        if (match != null) {
            _selectedCatalogFlagDetail.value = match
        }
    }

    fun closeFlagDetail() {
        _selectedCatalogFlagDetail.value = null
    }

    fun loadHistorySession(record: CompareHistoryRecord) {
        viewModelScope.launch {
            _isComparing.value = true
            try {
                val restored = repository.loadComparisonHistorySnapshot(record.id)
                if (restored != null) {
                    _comparisonReport.value = restored
                    _multiFileReport.value = null
                    _sessionDiffReport.value = null
                    _activeResultSection.value = CompareResultSection.OVERVIEW
                    _compareSectionTab.value = CompareSectionTab.CHANGES
                    _compareFilterTab.value = CompareFilterTab.ALL
                    _compareQuickFilter.value = CompareQuickFilter.ALL
                    rebuildSearchIndexAsync(restored)
                    _uiEvents.emit(UiEvent.ShowToast("Loaded saved comparison: ${record.oldSourceName} vs ${record.newSourceName}"))
                } else {
                    _uiEvents.emit(UiEvent.ShowToast("Snapshot file unavailable for this history entry"))
                }
            } finally {
                _isComparing.value = false
            }
        }
    }

    fun deleteHistorySession(id: String) {
        viewModelScope.launch {
            _compareHistory.value = repository.deleteComparisonHistoryItem(id)
            _uiEvents.emit(UiEvent.ShowToast("Removed history entry"))
        }
    }

    fun clearAllCompareHistory() {
        viewModelScope.launch {
            repository.clearAllComparisonHistory()
            _compareHistory.value = emptyList()
            _uiEvents.emit(UiEvent.ShowToast("Cleared comparison history"))
        }
    }

    fun copyText(text: String, label: String = "MetaConfig") {
        copyToClipboard(text, "Copied $label")
    }

    fun shareText(text: String, title: String) {
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShareText(text, title))
        }
    }

    fun shareComparisonReport(report: ComparisonReport) {
        val summaryText = MappingCompareEngine.generateTextReport(report)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShareText(summaryText, "Share Comparison Report"))
        }
    }

    fun exportComparisonReportTxt(report: ComparisonReport) {
        val text = MappingCompareEngine.generateTextReport(report)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShareText(text, "Export Comparison Report (Text)"))
        }
    }

    fun exportComparisonReportJson(report: ComparisonReport) {
        val json = MappingCompareEngine.generateJsonReport(report)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShareText(json, "Export Comparison Report (JSON)"))
        }
    }

    fun buildExportContentForReport(
        report: ComparisonReport,
        type: ExportReportType,
        format: ExportFileFormat,
        categoryOverride: String? = null,
        selectedOnly: Boolean = false
    ): String {
        if (selectedOnly && _selectedResultKeys.value.isNotEmpty()) {
            val selectedItems = report.catalogFlags.filter { _selectedResultKeys.value.contains(it.name) }
            return MappingIntelligenceAnalyzer.generateCatalogListExport(
                title = "Selected Results (${selectedItems.size})",
                items = selectedItems,
                format = format
            )
        }
        return when (type) {
            ExportReportType.SINGLE_CATEGORY_EXPORT -> {
                val cat = categoryOverride ?: _catalogCategoryFilter.value
                MappingIntelligenceAnalyzer.generateSingleCategoryExport(report, cat, format)
            }
            ExportReportType.MULTI_FILE_MATRIX_REPORT -> {
                val multi = _multiFileReport.value
                if (multi != null) {
                    MappingIntelligenceAnalyzer.generateMultiFileExport(multi, format)
                } else {
                    MappingCompareEngine.generateExportContent(report, type, format)
                }
            }
            else -> MappingCompareEngine.generateExportContent(report, type, format)
        }
    }

    fun saveExportToUri(
        uri: Uri,
        report: ComparisonReport,
        type: ExportReportType,
        format: ExportFileFormat,
        categoryOverride: String? = null,
        selectedOnly: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = buildExportContentForReport(
                    report = report,
                    type = type,
                    format = format,
                    categoryOverride = categoryOverride,
                    selectedOnly = selectedOnly
                )
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Could not open destination stream")
                _uiEvents.emit(
                    UiEvent.ShowToast(
                        if (selectedOnly) "Exported ${_selectedResultKeys.value.size} selected items (${format.extension.uppercase()})"
                        else "Saved ${type.title} (${format.extension.uppercase()})"
                    )
                )
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Save failed: ${e.message}"))
            }
        }
    }

    // ==========================================
    // Flag Analysis State & Management
    // ==========================================

    private val _flagAnalysisState = MutableStateFlow<ParsedBackupState?>(null)
    val flagAnalysisState: StateFlow<ParsedBackupState?> = _flagAnalysisState.asStateFlow()

    private val _isAnalyzingBackup = MutableStateFlow(false)
    val isAnalyzingBackup: StateFlow<Boolean> = _isAnalyzingBackup.asStateFlow()

    private val _flagAnalysisSearchQuery = MutableStateFlow("")
    val flagAnalysisSearchQuery: StateFlow<String> = _flagAnalysisSearchQuery.asStateFlow()

    private val _flagAnalysisFilter = MutableStateFlow(FlagAnalysisFilter.ALL)
    val flagAnalysisFilter: StateFlow<FlagAnalysisFilter> = _flagAnalysisFilter.asStateFlow()

    fun loadBackupForAnalysis(content: String, fileName: String) {
        viewModelScope.launch {
            _isAnalyzingBackup.value = true
            try {
                val mappingFlags = repository.getAllFlags()
                val result = FlagAnalysisEngine.parseBackup(content, fileName, mappingFlags)
                if (result.isSuccess) {
                    val state = result.getOrThrow()
                    _flagAnalysisState.value = state
                    _flagAnalysisSearchQuery.value = ""
                    _flagAnalysisFilter.value = FlagAnalysisFilter.ALL
                    _uiEvents.emit(
                        UiEvent.ShowToast(
                            "Loaded ${state.fileName}: ${state.totalUniversalFlags} flags, ${state.totalSubFlags} sub-flags"
                        )
                    )
                } else {
                    _uiEvents.emit(UiEvent.ShowToast("Analysis failed: ${result.exceptionOrNull()?.message}"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Error loading backup: ${e.message}"))
            } finally {
                _isAnalyzingBackup.value = false
            }
        }
    }

    fun toggleAnalysisSubFlag(universalCode: String, subCode: String) {
        val current = _flagAnalysisState.value ?: return
        val updatedFlags = FlagAnalysisEngine.toggleSubFlagState(current.flags, universalCode, subCode)
        _flagAnalysisState.value = current.copy(flags = updatedFlags)
    }

    fun setAnalysisCategoryState(category: String, enable: Boolean) {
        val current = _flagAnalysisState.value ?: return
        val updatedFlags = FlagAnalysisEngine.setCategoryFlagsState(current.flags, category, enable)
        _flagAnalysisState.value = current.copy(flags = updatedFlags)
        viewModelScope.launch {
            _uiEvents.emit(
                UiEvent.ShowToast("${if (enable) "Enabled" else "Disabled"} all flags in $category")
            )
        }
    }

    fun setAnalysisAllFlagsState(enable: Boolean) {
        val current = _flagAnalysisState.value ?: return
        val updatedFlags = FlagAnalysisEngine.setAllFlagsState(current.flags, enable)
        _flagAnalysisState.value = current.copy(flags = updatedFlags)
        viewModelScope.launch {
            _uiEvents.emit(
                UiEvent.ShowToast("${if (enable) "Enabled" else "Disabled"} all flags in backup")
            )
        }
    }

    fun resetAnalysisSubFlag(universalCode: String, subCode: String) {
        val current = _flagAnalysisState.value ?: return
        val updatedFlags = FlagAnalysisEngine.resetSubFlagToOriginal(current.flags, universalCode, subCode)
        _flagAnalysisState.value = current.copy(flags = updatedFlags)
    }

    fun resetAllAnalysisFlags() {
        val current = _flagAnalysisState.value ?: return
        val updatedFlags = FlagAnalysisEngine.resetAllFlagsToOriginal(current.flags)
        _flagAnalysisState.value = current.copy(flags = updatedFlags)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Reset all modified flags to original state"))
        }
    }

    fun setFlagAnalysisSearchQuery(query: String) {
        _flagAnalysisSearchQuery.value = query
    }

    fun setFlagAnalysisFilter(filter: FlagAnalysisFilter) {
        _flagAnalysisFilter.value = filter
    }

    fun clearFlagAnalysis() {
        _flagAnalysisState.value = null
        _flagAnalysisSearchQuery.value = ""
        _flagAnalysisFilter.value = FlagAnalysisFilter.ALL
    }
}
