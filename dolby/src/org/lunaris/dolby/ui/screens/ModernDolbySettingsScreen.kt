/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.lunaris.dolby.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.lunaris.dolby.R
import org.lunaris.dolby.domain.models.DolbyUiState
import org.lunaris.dolby.ui.components.AppProfileSettingsCard
import org.lunaris.dolby.ui.components.CreditsDialog
import org.lunaris.dolby.ui.components.DolbyMainCard
import org.lunaris.dolby.ui.components.DolbyPageHeading
import org.lunaris.dolby.ui.components.DolbyTopAppBar
import org.lunaris.dolby.ui.components.ExpressiveErrorState
import org.lunaris.dolby.ui.components.ExpressiveLoadingState
import org.lunaris.dolby.ui.components.ModernConfirmDialog
import org.lunaris.dolby.ui.components.ModernIeqSelector
import org.lunaris.dolby.ui.components.ModernProfileSelector
import org.lunaris.dolby.ui.components.NotificationListenerPermissionCard
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
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 8 }
    }
    var showResetDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DolbyTopAppBar(
                title = stringResource(R.string.dolby_title),
                showTitle = showTitle || uiState !is DolbyUiState.Success,
                scrolled = isScrolled,
                actions = {
                    FilledTonalIconButton(
                        onClick = { showCreditsDialog = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.dolby_credits)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = stringResource(R.string.dolby_reset_all)
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
                        ExpressiveLoadingState(message = stringResource(R.string.loading))
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
                        ExpressiveErrorState(
                            message = state.message,
                            onRetry = { viewModel.loadSettings() }
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        ModernConfirmDialog(
            title = stringResource(R.string.dolby_reset_all),
            message = stringResource(R.string.dolby_reset_all_message),
            icon = Icons.Rounded.RestartAlt,
            isDestructive = true,
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
    val profileEntries = stringArrayResource(R.array.dolby_profile_entries)
    val profileValues = stringArrayResource(R.array.dolby_profile_values)
    val activeProfileIndex = profileValues.indexOfFirst { it.toIntOrNull() == state.settings.currentProfile }
    val activeProfileName = profileEntries.getOrElse(activeProfileIndex.coerceAtLeast(0)) { "" }

    val statusText = if (state.settings.enabled && activeProfileName.isNotEmpty()) {
        stringResource(R.string.dolby_on_with_profile, activeProfileName)
    } else {
        stringResource(if (state.settings.enabled) R.string.dolby_on else R.string.dolby_off)
    }
    val outputDeviceText = stringResource(
        if (state.isOnSpeaker) R.string.dolby_output_speaker else R.string.dolby_output_headphones
    )
    val outputDeviceIcon = if (state.isOnSpeaker) Icons.Rounded.Speaker else Icons.Rounded.Headphones

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
                        statusText = statusText,
                        outputDeviceText = outputDeviceText,
                        outputDeviceIcon = outputDeviceIcon,
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
                        onManageClick = { navController.navigate(Screen.AppProfiles.route) },
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}
