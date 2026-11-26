package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CompareSearchIndex
import com.example.data.MappingCompareEngine
import com.example.data.MappingIntelligenceAnalyzer
import com.example.data.MetaConfigParser
import com.example.model.CatalogDisplayMode
import com.example.model.CatalogFlagItem
import com.example.model.CatalogScopeFilter
import com.example.model.CatalogSortOption
import com.example.model.CompareQuickFilter
import com.example.model.ComparisonReport
import com.example.model.FlagChangeStatus
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenContainer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledCardHover
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.UpdatedOrange
import com.example.ui.theme.WarningYellow

@Composable
fun MappingCatalogSection(
    report: ComparisonReport,
    searchQuery: String,
    quickFilter: CompareQuickFilter,
    selectedCategory: String,
    selectedCategories: Set<String> = emptySet(),
    sortOption: CatalogSortOption,
    scopeFilter: CatalogScopeFilter,
    onCategorySelected: (String) -> Unit,
    onSortSelected: (CatalogSortOption) -> Unit,
    onScopeSelected: (CatalogScopeFilter) -> Unit,
    onOpenDetail: (CatalogFlagItem) -> Unit,
    onCopy: (String, String) -> Unit,
    namespaceFilter: String = "All",
    exactSubflagCount: Int? = null,
    onlyInOneMapping: Boolean = false,
    favoritesOnly: Boolean = false,
    favoriteFlagNames: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    displayMode: CatalogDisplayMode = CatalogDisplayMode.EXPANDED,
    onDisplayModeChanged: (CatalogDisplayMode) -> Unit = {},
    expandAllSubflags: Boolean = false,
    onToggleExpandAll: () -> Unit = {},
    selectedKeys: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    onOpenMultiCategoryDialog: () -> Unit = {},
    searchIndex: CompareSearchIndex? = null,
    modifier: Modifier = Modifier
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var visibleLimit by remember(
        selectedCategory, selectedCategories, searchQuery, quickFilter, sortOption,
        scopeFilter, namespaceFilter, exactSubflagCount, onlyInOneMapping, favoritesOnly
    ) {
        mutableIntStateOf(60)
    }

    val categoryCounts = remember(report.categoryAnalytics) {
        report.categoryAnalytics.associateBy { it.categoryName }
    }

    val filteredCatalog = remember(
        report.catalogFlags,
        searchIndex,
        searchQuery,
        quickFilter,
        selectedCategory,
        selectedCategories,
        sortOption,
        namespaceFilter,
        exactSubflagCount,
        onlyInOneMapping,
        favoritesOnly,
        favoriteFlagNames
    ) {
        val indexByFlagName = searchIndex?.entries?.associateBy { it.item.name }

        val base = report.catalogFlags.filter { item ->
            val matchesCat = when {
                selectedCategories.isNotEmpty() -> selectedCategories.contains(item.category)
                selectedCategory != "See All Flags" -> item.category == selectedCategory
                else -> true
            }
            if (!matchesCat) return@filter false

            if (namespaceFilter != "All" && !item.namespacePrefix.equals(namespaceFilter, ignoreCase = true)) {
                return@filter false
            }
            if (exactSubflagCount != null && item.totalDistinctSubFlags != exactSubflagCount) {
                return@filter false
            }
            if (onlyInOneMapping && !item.isOnlyInOneMapping) {
                return@filter false
            }
            if (favoritesOnly && !favoriteFlagNames.contains(item.name)) {
                return@filter false
            }

            val matchesQuick = when (quickFilter) {
                CompareQuickFilter.ALL -> true
                CompareQuickFilter.NEW -> item.status == FlagChangeStatus.NEW
                CompareQuickFilter.REMOVED -> item.status == FlagChangeStatus.REMOVED
                CompareQuickFilter.UPDATED -> item.status == FlagChangeStatus.UPDATED
                CompareQuickFilter.UNCHANGED -> item.status == FlagChangeStatus.UNCHANGED
                CompareQuickFilter.HAS_SUBFLAGS -> item.totalDistinctSubFlags > 0
                CompareQuickFilter.NO_SUBFLAGS -> item.totalDistinctSubFlags == 0
                CompareQuickFilter.DUPLICATE -> item.isDuplicateInOld || item.isDuplicateInNew
                CompareQuickFilter.INVALID -> false
                CompareQuickFilter.UNKNOWN_CATEGORY -> item.isUnknownCategory
            }
            if (!matchesQuick) return@filter false

            if (searchQuery.isBlank()) {
                true
            } else {
                val indexed = indexByFlagName?.get(item.name)
                if (indexed != null) {
                    MappingIntelligenceAnalyzer.matchesEntryAdvanced(indexed, searchQuery)
                } else {
                    MappingCompareEngine.matchesQuery(
                        searchQuery,
                        item.name,
                        item.oldCode,
                        item.newCode,
                        item.category,
                        item.namespacePrefix
                    ) || item.subFlags.any { sub ->
                        MappingCompareEngine.matchesQuery(searchQuery, sub.name, sub.oldCode, sub.newCode)
                    }
                }
            }
        }

        when (sortOption) {
            CatalogSortOption.ALPHABETICAL -> base.sortedBy { it.name }
            CatalogSortOption.ALPHA_SUBFLAG -> base.sortedBy {
                it.subFlags.minOfOrNull { sf -> sf.name } ?: "zzzz_${it.name}"
            }
            CatalogSortOption.CODE -> base.sortedBy { it.displayCode.toLongOrNull() ?: Long.MAX_VALUE }
            CatalogSortOption.SUBFLAG_CODE -> base.sortedBy {
                it.subFlags.minOfOrNull { sf ->
                    (sf.newCode ?: sf.oldCode)?.toLongOrNull() ?: Long.MAX_VALUE
                } ?: Long.MAX_VALUE
            }
            CatalogSortOption.CATEGORY -> base.sortedWith(compareBy({ it.category }, { it.name }))
            CatalogSortOption.SUBFLAG_COUNT -> base.sortedByDescending { it.totalDistinctSubFlags }
            CatalogSortOption.LOWEST_SUBFLAG_COUNT -> base.sortedBy { it.totalDistinctSubFlags }
            CatalogSortOption.STATUS -> base.sortedWith(compareBy({ it.status.ordinal }, { it.name }))
        }
    }

    val subFlagOnlyItems = remember(filteredCatalog, scopeFilter, searchQuery, sortOption) {
        if (scopeFilter != CatalogScopeFilter.SUBFLAGS_ONLY) {
            emptyList()
        } else {
            val flattened = filteredCatalog.flatMap { parent ->
                parent.subFlags.filter { sub ->
                    searchQuery.isBlank() ||
                        MappingCompareEngine.matchesQuery(
                            searchQuery,
                            sub.name,
                            sub.oldCode,
                            sub.newCode,
                            parent.name,
                            parent.category
                        )
                }.map { sub -> parent to sub }
            }
            when (sortOption) {
                CatalogSortOption.ALPHA_SUBFLAG -> flattened.sortedBy { it.second.name }
                CatalogSortOption.SUBFLAG_CODE -> flattened.sortedBy {
                    (it.second.newCode ?: it.second.oldCode)?.toLongOrNull() ?: Long.MAX_VALUE
                }
                else -> flattened
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Category Picker + Multi-Category Button + Sort Picker Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1.3f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmoledCard)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                        .clickable { categoryMenuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                        .testTag("catalog_category_selector"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelText = when {
                        selectedCategories.size > 1 -> "${selectedCategories.size} Categories"
                        selectedCategory == "See All Flags" -> "All Flags (${report.catalogFlags.size})"
                        else -> "$selectedCategory (${categoryCounts[selectedCategory]?.totalUniqueFlags ?: 0})"
                    }
                    Text(
                        text = labelText,
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select category",
                        tint = AccentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier
                        .background(AmoledCard)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                ) {
                    MetaConfigParser.CATEGORY_NAMES.forEach { catName ->
                        val catCount = if (catName == "See All Flags") {
                            report.catalogFlags.size
                        } else {
                            categoryCounts[catName]?.totalUniqueFlags ?: 0
                        }
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = catName,
                                        color = if (catName == selectedCategory) AccentGreen else TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (catName == selectedCategory) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "$catCount",
                                        color = TextTertiary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            },
                            onClick = {
                                onCategorySelected(catName)
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Multi-Category Selector Trigger
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedCategories.size > 1) AccentGreenContainer else AmoledCard)
                    .border(
                        1.dp,
                        if (selectedCategories.size > 1) AccentGreen else AmoledBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onOpenMultiCategoryDialog)
                    .padding(horizontal = 9.dp, vertical = 9.dp)
                    .testTag("button_multi_category_filter")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Multi-category filter",
                    tint = if (selectedCategories.size > 1) AccentGreen else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Sort Dropdown
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmoledCard)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 9.dp)
                        .testTag("catalog_sort_selector"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sortOption.label,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Sort options",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier
                        .background(AmoledCard)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                ) {
                    CatalogSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    color = if (option == sortOption) AccentGreen else TextPrimary,
                                    fontSize = 12.5.sp
                                )
                            },
                            onClick = {
                                onSortSelected(option)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Scope Toggle + Display Mode (Compact / Expanded) + Expand All / Collapse All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CatalogScopeFilter.entries.forEach { scope ->
                val isSelected = scopeFilter == scope
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) AccentGreenContainer else AmoledCard)
                        .border(1.dp, if (isSelected) AccentGreen else AmoledBorder, RoundedCornerShape(6.dp))
                        .clickable { onScopeSelected(scope) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = scope.label,
                        color = if (isSelected) AccentGreen else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Compact vs Expanded Display Mode Toggle
            val isCompact = displayMode == CatalogDisplayMode.COMPACT
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isCompact) AccentGreenContainer else AmoledCard)
                    .border(1.dp, if (isCompact) AccentGreen else AmoledBorder, RoundedCornerShape(6.dp))
                    .clickable {
                        onDisplayModeChanged(
                            if (isCompact) CatalogDisplayMode.EXPANDED else CatalogDisplayMode.COMPACT
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("toggle_catalog_display_mode"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isCompact) Icons.Default.ViewHeadline else Icons.Default.ViewAgenda,
                    contentDescription = "Toggle compact mode",
                    tint = if (isCompact) AccentGreen else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = displayMode.label,
                    color = if (isCompact) AccentGreen else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Expand All / Collapse All Subflags
            if (scopeFilter == CatalogScopeFilter.ALL) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (expandAllSubflags) AccentGreenContainer else AmoledCard)
                        .border(1.dp, if (expandAllSubflags) AccentGreen else AmoledBorder, RoundedCornerShape(6.dp))
                        .clickable(onClick = onToggleExpandAll)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("toggle_expand_all_catalog"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (expandAllSubflags) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = null,
                        tint = if (expandAllSubflags) AccentGreen else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (expandAllSubflags) "Collapse All" else "Expand All",
                        color = if (expandAllSubflags) AccentGreen else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Count Header
        val totalMatched = if (scopeFilter == CatalogScopeFilter.SUBFLAGS_ONLY) subFlagOnlyItems.size else filteredCatalog.size
        Text(
            text = "Showing ${minOf(visibleLimit, totalMatched)} of $totalMatched matching entries (Tap flag for detail, ★ pin favorite, ☑ multi-select)",
            color = TextTertiary,
            fontSize = 11.sp
        )

        if (totalMatched == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No catalog entries match the active filters",
                    color = TextTertiary,
                    fontSize = 13.sp
                )
            }
        } else if (scopeFilter == CatalogScopeFilter.SUBFLAGS_ONLY) {
            subFlagOnlyItems.take(visibleLimit).forEach { (parent, sub) ->
                val statusColor = statusColor(sub.status)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmoledCard)
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { onOpenDetail(parent) }
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sub.status.label.uppercase(),
                            color = statusColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${parent.category} • ${parent.namespacePrefix}",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sub.name,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onCopy(sub.name, "Subflag Name") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy subflag name",
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Parent: ${parent.name} • Old=${sub.oldCode ?: "-"} | New=${sub.newCode ?: "-"}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            filteredCatalog.take(visibleLimit).forEach { item ->
                CatalogUniversalCard(
                    item = item,
                    showExpandSubFlags = scopeFilter == CatalogScopeFilter.ALL,
                    forceExpanded = expandAllSubflags,
                    isCompact = displayMode == CatalogDisplayMode.COMPACT,
                    isFavorite = favoriteFlagNames.contains(item.name),
                    isSelected = selectedKeys.contains(item.name),
                    onToggleFavorite = { onToggleFavorite(item.name) },
                    onToggleSelection = { onToggleSelection(item.name) },
                    onOpenDetail = { onOpenDetail(item) },
                    onCopy = onCopy
                )
            }
        }

        if (totalMatched > visibleLimit) {
            OutlinedButton(
                onClick = { visibleLimit += 100 },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "Load More (+${minOf(100, totalMatched - visibleLimit)} of ${totalMatched - visibleLimit} remaining)",
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CatalogUniversalCard(
    item: CatalogFlagItem,
    showExpandSubFlags: Boolean,
    forceExpanded: Boolean = false,
    isCompact: Boolean = false,
    isFavorite: Boolean = false,
    isSelected: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onToggleSelection: () -> Unit = {},
    onOpenDetail: () -> Unit,
    onCopy: (String, String) -> Unit
) {
    var localExpanded by remember(item.name) { mutableStateOf(false) }
    val expanded = forceExpanded || localExpanded
    val accent = statusColor(item.status)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) AccentGreenContainer.copy(alpha = 0.25f) else AmoledCard)
            .border(
                1.dp,
                if (isSelected) AccentGreen else accent.copy(alpha = 0.45f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onOpenDetail)
            .padding(if (isCompact) 9.dp else 12.dp)
            .testTag("catalog_flag_${item.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Select entry",
                    tint = if (isSelected) AccentGreen else TextTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onToggleSelection)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.status.label.uppercase(),
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = item.category,
                    color = InfoBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                if (item.isDuplicateInOld || item.isDuplicateInNew) {
                    Text(
                        text = "DUPLICATE",
                        color = WarningYellow,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = if (isFavorite) "Unpin favorite" else "Pin favorite",
                    tint = if (isFavorite) WarningYellow else TextTertiary,
                    modifier = Modifier
                        .size(19.dp)
                        .clickable(onClick = onToggleFavorite)
                )

                if (!isCompact && showExpandSubFlags && item.subFlags.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { localExpanded = !localExpanded }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.totalDistinctSubFlags} subs",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand subflags",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCopy(item.name, "Flag Name") }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.name,
                color = TextPrimary,
                fontSize = if (isCompact) 12.sp else 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = if (isCompact) 1 else 3
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy flag name",
                tint = TextTertiary,
                modifier = Modifier.size(13.dp)
            )
        }

        if (!isCompact) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.availability.label} • ${item.namespacePrefix}",
                    color = TextTertiary,
                    fontSize = 10.sp
                )
                Text(
                    text = "Old: ${item.oldCode ?: "-"}  •  New: ${item.newCode ?: "-"}",
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Text(
                text = "Old ${item.oldCode ?: "-"} -> New ${item.newCode ?: "-"} • ${item.totalDistinctSubFlags} subs",
                color = TextTertiary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        AnimatedVisibility(visible = expanded && showExpandSubFlags && !isCompact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HorizontalDivider(color = AmoledBorder)
                item.subFlags.forEach { sub ->
                    val sColor = statusColor(sub.status)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sub.name,
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Old: ${sub.oldCode ?: "-"} -> New: ${sub.newCode ?: "-"}",
                                color = TextTertiary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sub.status.label.uppercase(),
                                color = sColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy subflag name",
                                tint = TextTertiary,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable { onCopy(sub.name, "Subflag Name") }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun statusColor(status: FlagChangeStatus): Color = when (status) {
    FlagChangeStatus.NEW -> AccentGreen
    FlagChangeStatus.REMOVED -> DangerRed
    FlagChangeStatus.UPDATED -> UpdatedOrange
    FlagChangeStatus.UNCHANGED -> TextSecondary
}

