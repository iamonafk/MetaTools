package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.FlagCategory
import com.example.model.FlagSearchResult
import com.example.model.MappingSource
import com.example.model.MatchType
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import com.example.ui.components.CategoryCard
import com.example.ui.components.FlagCard
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenContainer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledBorderHighlight
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledCardHover
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.MetaToolsViewModel

@Composable
fun FlagsScreen(
    viewModel: MetaToolsViewModel,
    modifier: Modifier = Modifier
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val mappingSource by viewModel.mappingSource.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val expandedCategories by viewModel.expandedCategories.collectAsStateWithLifecycle()
    val expandedFlags by viewModel.expandedFlags.collectAsStateWithLifecycle()
    val categoryFlagsMap by viewModel.categoryFlagsMap.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
    ) {
        // Top Search Field & Controls Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Rounded Search Field (AMOLED Dark Gray with subtle border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(AmoledCard)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(25.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotEmpty()) AccentGreen else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search flags, universe, sub-flags...",
                                color = TextTertiary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(AccentGreen),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    viewModel.executeSearch(searchQuery)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("flags_search_input")
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearSearch() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mapping Source Status & Switch bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmoledCardHover)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                    .clickable { viewModel.openSourceDialog() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (mappingSource?.isCustom == true) "Custom: " else "Built-in: ",
                        color = TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = mappingSource?.name ?: "Loading...",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentGreenContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${mappingSource?.totalFlags ?: 0} flags",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch mapping",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Recent Searches Chips (when search is not empty or active)
            if (recentSearches.isNotEmpty() && searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(recentSearches) { query ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmoledCard)
                                .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.executeSearch(query) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = query,
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Main Content Area
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = AccentGreen,
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading and indexing MetaConfigs...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (searchQuery.trim().isNotEmpty()) {
            // Search Results Mode
            SearchResultsView(
                query = searchQuery,
                results = searchResults,
                isSearching = isSearching,
                expandedFlagCodes = expandedFlags,
                onToggleFlag = { viewModel.toggleFlagExpansion(it) },
                onCopyUniversalName = { viewModel.copyUniversalFlagName(it) },
                onCopyUniversalCode = { viewModel.copyUniversalFlagCode(it) },
                onCopySubFlagName = { viewModel.copySubFlagName(it) },
                onCopySubFlagCode = { viewModel.copySubFlagCode(it) },
                onShareFlag = { viewModel.shareUniversalFlag(it) },
                onShareSubFlag = { viewModel.shareSubFlag(it) }
            )
        } else {
            // Categories List Mode
            CategoriesListView(
                categories = categories,
                expandedCategoryIds = expandedCategories,
                expandedFlagCodes = expandedFlags,
                categoryFlagsMap = categoryFlagsMap,
                onToggleCategory = { viewModel.toggleCategory(it) },
                onToggleFlag = { viewModel.toggleFlagExpansion(it) },
                onCopyUniversalName = { viewModel.copyUniversalFlagName(it) },
                onCopyUniversalCode = { viewModel.copyUniversalFlagCode(it) },
                onCopySubFlagName = { viewModel.copySubFlagName(it) },
                onCopySubFlagCode = { viewModel.copySubFlagCode(it) },
                onShareFlag = { viewModel.shareUniversalFlag(it) },
                onShareSubFlag = { viewModel.shareSubFlag(it) }
            )
        }
    }
}

@Composable
private fun CategoriesListView(
    categories: List<FlagCategory>,
    expandedCategoryIds: Set<String>,
    expandedFlagCodes: Set<String>,
    categoryFlagsMap: Map<String, List<UniversalFlag>>,
    onToggleCategory: (String) -> Unit,
    onToggleFlag: (String) -> Unit,
    onCopyUniversalName: (UniversalFlag) -> Unit,
    onCopyUniversalCode: (UniversalFlag) -> Unit,
    onCopySubFlagName: (SubFlag) -> Unit,
    onCopySubFlagCode: (SubFlag) -> Unit,
    onShareFlag: (UniversalFlag) -> Unit,
    onShareSubFlag: (SubFlag) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("categories_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            val isExpanded = expandedCategoryIds.contains(category.id)
            val flags = categoryFlagsMap[category.id]

            CategoryCard(
                category = category,
                isExpanded = isExpanded,
                flags = flags,
                expandedFlagCodes = expandedFlagCodes,
                onToggleCategory = { onToggleCategory(category.id) },
                onToggleFlag = onToggleFlag,
                onCopyUniversalName = onCopyUniversalName,
                onCopyUniversalCode = onCopyUniversalCode,
                onCopySubFlagName = onCopySubFlagName,
                onCopySubFlagCode = onCopySubFlagCode,
                onShareFlag = onShareFlag,
                onShareSubFlag = onShareSubFlag
            )
        }
    }
}

@Composable
private fun SearchResultsView(
    query: String,
    results: List<FlagSearchResult>,
    isSearching: Boolean,
    expandedFlagCodes: Set<String>,
    onToggleFlag: (String) -> Unit,
    onCopyUniversalName: (UniversalFlag) -> Unit,
    onCopyUniversalCode: (UniversalFlag) -> Unit,
    onCopySubFlagName: (SubFlag) -> Unit,
    onCopySubFlagCode: (SubFlag) -> Unit,
    onShareFlag: (UniversalFlag) -> Unit,
    onShareSubFlag: (SubFlag) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Result summary header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SEARCH RESULTS",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSearching) {
                    CircularProgressIndicator(
                        color = AccentGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = "${results.size} match${if (results.size == 1) "" else "es"}",
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (results.isEmpty() && !isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No flags match \"$query\"",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try searching by universal flag code, sub-flag name, or token words in any order",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("search_results_list"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = results,
                    key = { result ->
                        if (result.matchedSubFlag != null) {
                            "sr_${result.universalFlag.code}_${result.matchedSubFlag.code}_${result.matchedSubFlag.name}"
                        } else {
                            "sr_${result.universalFlag.code}"
                        }
                    }
                ) { result ->
                    val flag = result.universalFlag
                    val isExpanded = expandedFlagCodes.contains(flag.code)

                    Column {
                        // Category pill above card
                        Row(
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AmoledCardHover)
                                    .border(1.dp, AmoledBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = flag.category,
                                    color = TextSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (result.matchType == MatchType.SUBFLAG_NAME || result.matchType == MatchType.SUBFLAG_CODE) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Matched Sub-Flag",
                                    color = AccentGreen,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        FlagCard(
                            flag = flag,
                            isExpanded = isExpanded || result.matchedSubFlag != null,
                            onToggleExpand = { onToggleFlag(flag.code) },
                            onCopyUniversalName = { onCopyUniversalName(flag) },
                            onCopyUniversalCode = { onCopyUniversalCode(flag) },
                            onCopySubFlagName = onCopySubFlagName,
                            onCopySubFlagCode = onCopySubFlagCode,
                            onShareFlag = { onShareFlag(flag) },
                            onShareSubFlag = onShareSubFlag,
                            highlightedSubFlag = result.matchedSubFlag
                        )
                    }
                }
            }
        }
    }
}
