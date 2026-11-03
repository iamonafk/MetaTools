package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FlagAnalysisEngine
import com.example.data.MappingCompareEngine
import com.example.data.MappingResult
import com.example.data.MetaConfigRepository
import com.example.model.CompareFilterTab
import com.example.model.CompareSource
import com.example.model.ComparisonReport
import com.example.model.FlagAnalysisFilter
import com.example.model.FlagCategory
import com.example.model.FlagSearchResult
import com.example.model.MappingSource
import com.example.model.ParsedBackupState
import com.example.model.SubFlag
import com.example.model.UniversalFlag
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

    private val _isComparing = MutableStateFlow(false)
    val isComparing: StateFlow<Boolean> = _isComparing.asStateFlow()

    private val _comparisonReport = MutableStateFlow<ComparisonReport?>(null)
    val comparisonReport: StateFlow<ComparisonReport?> = _comparisonReport.asStateFlow()

    private val _compareFilterTab = MutableStateFlow(CompareFilterTab.ALL)
    val compareFilterTab: StateFlow<CompareFilterTab> = _compareFilterTab.asStateFlow()

    private val _compareSearchQuery = MutableStateFlow("")
    val compareSearchQuery: StateFlow<String> = _compareSearchQuery.asStateFlow()

    init {
        loadRecentSearches()
        loadInitialData()

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
        }
    }

    fun setOldCompareSource(source: CompareSource) {
        _oldCompareSource.value = source
    }

    fun setNewCompareSource(source: CompareSource) {
        _newCompareSource.value = source
    }

    fun setOldCompareSourceFromUri(uri: Uri, displayName: String) {
        _oldCompareSource.value = CompareSource.DeviceFile(uri, displayName)
    }

    fun setNewCompareSourceFromUri(uri: Uri, displayName: String) {
        _newCompareSource.value = CompareSource.DeviceFile(uri, displayName)
    }

    fun swapCompareSources() {
        val currentOld = _oldCompareSource.value
        val currentNew = _newCompareSource.value
        if (currentNew != null) {
            _oldCompareSource.value = currentNew
            _newCompareSource.value = currentOld
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

    fun clearComparison() {
        _comparisonReport.value = null
        _compareSearchQuery.value = ""
        _compareFilterTab.value = CompareFilterTab.ALL
    }

    fun setCompareFilterTab(tab: CompareFilterTab) {
        _compareFilterTab.value = tab
    }

    fun onCompareSearchQueryChanged(query: String) {
        _compareSearchQuery.value = query
    }

    fun clearCompareSearch() {
        _compareSearchQuery.value = ""
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
