package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MappingSourceDialog
import com.example.ui.components.StickyBottomNavBar
import com.example.ui.components.StickyTopHeader
import com.example.ui.screens.FlagsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.MetaToolsTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.MetaToolsViewModel
import com.example.viewmodel.UiEvent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MetaToolsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MetaToolsTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MetaToolsViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val showSettingsSheet by viewModel.showSettingsSheet.collectAsStateWithLifecycle()
    val showSourceDialog by viewModel.showSourceDialog.collectAsStateWithLifecycle()
    val mappingSource by viewModel.mappingSource.collectAsStateWithLifecycle()
    val hasCustomMapping by viewModel.hasCustomMapping.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // SAF File picker launcher for custom JSON mapping
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val displayName = queryDisplayName(context, uri) ?: "custom_mapping.json"
            viewModel.importCustomMapping(uri, displayName)
        }
    }

    // Handle back button navigation
    BackHandler {
        if (showSettingsSheet) {
            viewModel.closeSettings()
        } else if (searchQuery.isNotEmpty()) {
            viewModel.clearSearch()
        } else if (currentTab != AppTab.FLAGS) {
            viewModel.selectTab(AppTab.FLAGS)
        } else {
            (context as? ComponentActivity)?.finish()
        }
    }

    // Collect UI events (Toast, Native Share)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ShareText -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, event.title))
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground),
        containerColor = AmoledBackground,
        topBar = {
            StickyTopHeader(
                onSearchClick = {
                    if (showSettingsSheet) {
                        viewModel.closeSettings()
                    }
                    viewModel.selectTab(AppTab.FLAGS)
                },
                onSettingsClick = {
                    if (showSettingsSheet) {
                        viewModel.closeSettings()
                    } else {
                        viewModel.openSettings()
                    }
                }
            )
        },
        bottomBar = {
            StickyBottomNavBar(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    if (showSettingsSheet) {
                        viewModel.closeSettings()
                    }
                    viewModel.selectTab(tab)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmoledBackground)
        ) {
            if (showSettingsSheet) {
                SettingsScreen(
                    viewModel = viewModel,
                    onClose = { viewModel.closeSettings() }
                )
            } else {
                when (currentTab) {
                    AppTab.HOME -> HomeScreen(viewModel = viewModel)
                    AppTab.FLAGS -> FlagsScreen(viewModel = viewModel)
                    AppTab.TOOLS -> ToolsScreen(viewModel = viewModel)
                    AppTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Mapping Source Selector Dialog
    if (showSourceDialog) {
        MappingSourceDialog(
            currentSource = mappingSource,
            hasCustomMapping = hasCustomMapping,
            onDismiss = { viewModel.closeSourceDialog() },
            onImportClick = {
                filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            onSwitchToCustom = {
                viewModel.switchToCustomMapping()
            },
            onRestoreBuiltIn = {
                viewModel.restoreBuiltInMapping()
            }
        )
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        return cursor.getString(index)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
    return uri.path?.substringAfterLast('/')
}
