/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.lunaris.dolby.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.lunaris.dolby.R
import org.lunaris.dolby.domain.models.DolbyUiState
import org.lunaris.dolby.ui.components.*
import org.lunaris.dolby.ui.viewmodel.DolbyViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernDolbySettingsScreen(
    viewModel: DolbyViewModel,
    navController: NavController,
    bottomPadding: Dp = 0.dp,
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val showTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    var showResetDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DolbyTopAppBar(
                title = stringResource(R.string.dolby_title),
                showTitle = showTitle || uiState !is DolbyUiState.Success,
                actions = {
                    IconButton(onClick = { showCreditsDialog = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.dolby_credits),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.dolby_reset_all),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is DolbyUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is DolbyUiState.Success -> {
                ModernDolbySettingsContent(
                    state = state,
                    listState = listState,
                    viewModel = viewModel,
                    navController = navController,
                    bottomPadding = bottomPadding,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is DolbyUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        }

    }
    if (showResetDialog) {
        ModernConfirmDialog(
            title = stringResource(R.string.dolby_reset_all),
            message = stringResource(R.string.dolby_reset_all_message),
            icon = Icons.Default.RestartAlt,
            onConfirm = {
                viewModel.resetAllProfiles()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showCreditsDialog) {
        CreditsDialog(
            onDismiss = { showCreditsDialog = false }
        )
    }
}

@Composable
private fun ModernDolbySettingsContent(
    state: DolbyUiState.Success,
    listState: LazyListState,
    viewModel: DolbyViewModel,
    navController: NavController,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val twoColumns = maxWidth / LocalDensity.current.fontScale >= 720.dp
        LazyColumn(
            modifier = Modifier.widthIn(max = 1040.dp).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + bottomPadding,
            ),
        ) {
            item(key = "heading") {
                DolbyPageHeading(
                    title = stringResource(R.string.dolby_title),
                    subtitle = stringResource(R.string.dolby_summary),
                )
            }
            item(key = "power") {
                Box(Modifier.padding(top = 12.dp)) {
                    DolbyMainCard(
                        enabled = state.settings.enabled,
                        onEnabledChange = { viewModel.setDolbyEnabled(it) },
                    )
                }
            }
            if (state.settings.enabled) {
                item(key = "sound_controls") {
                    val profiles: @Composable () -> Unit = {
                        ModernProfileSelector(
                            currentProfile = state.settings.currentProfile,
                            onProfileChange = { viewModel.setProfile(it) },
                        )
                    }
                    val equalizer: @Composable () -> Unit = {
                        ModernIeqSelector(
                            currentPreset = state.profileSettings.ieqPreset,
                            onPresetChange = { viewModel.setIeqPreset(it) },
                        )
                    }
                    val showEqualizer = state.settings.currentProfile != 0
                    if (twoColumns && showEqualizer) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(Modifier.weight(1f)) { profiles() }
                            Box(Modifier.weight(1f)) { equalizer() }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            profiles()
                            if (showEqualizer) equalizer()
                        }
                    }
                }
            }
            item(key = "notification_access") {
                NotificationListenerPermissionCard(modifier = Modifier.padding(top = 16.dp))
            }
            if (state.settings.enabled) {
                item(key = "app_profiles") {
                    AppProfileSettingsCard(
                        onManageClick = { navController.navigate("app_profiles") },
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}
