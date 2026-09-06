package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.example.core.model.NetworkType
import com.example.core.viewmodel.VlastMechanicsViewModel
import com.example.ui.components.VlastConfirmationDialog
import com.example.ui.dialogs.LimitInputDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HotspotScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.VlastTheme
import com.example.ui.theme.VlastTokens
import kotlinx.coroutines.launch

/**
 * Main Application Navigation & Screen Shell:
 *
 * Implements:
 * - Native RTL Layout (Item 14)
 * - Fixed Bottom Navigation Bar (Item 13)
 *   (الرئيسية / التقارير / نقطة الاتصال / الإعدادات)
 * - Confirmation and input dialog handlers
 * - Integrated Vlast Theme and Dark mode default (Item 2)
 */

enum class VlastNavDestination(val title: String, val icon: ImageVector) {
    HOME("الرئيسية", Icons.Filled.Home),
    REPORTS("التقارير", Icons.Filled.BarChart),
    HOTSPOT("نقطة الاتصال", Icons.Filled.WifiTethering),
    SETTINGS("الإعدادات", Icons.Filled.Settings)
}

@Composable
fun VlastAppNavigationShell(
    viewModel: VlastMechanicsViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(VlastNavDestination.HOME) }
    var isDarkMode by remember { mutableStateOf(true) } // Item 2: Dark mode default
    var isCutSoundEnabled by remember { mutableStateOf(true) } // Item 29: Dedicated sound toggle

    val todayRecord by viewModel.todayRecord.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activeNetwork by viewModel.activeNetwork.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val priorityStatus by viewModel.currentPriorityStatus.collectAsState()
    val undoState by viewModel.undoState.collectAsState()
    val activeConfirmation by viewModel.activeConfirmation.collectAsState()
    val copyMessage by viewModel.copyMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(copyMessage) {
        copyMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Dialog state
    var editDialogTarget by remember { mutableStateOf<NetworkType?>(null) }
    var isEditingTodayOverride by remember { mutableStateOf(false) }
    var isEditingHotspotThreshold by remember { mutableStateOf(false) }

    // Native Right-to-Left (RTL) Layout Provider (Item 14)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        VlastTheme(darkTheme = isDarkMode) {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    // Item 13: Fixed Bottom Navigation Bar
                    NavigationBar(
                        containerColor = VlastTokens.DarkSurface,
                        contentColor = VlastTokens.TextPrimary
                    ) {
                        VlastNavDestination.values().forEach { destination ->
                            val isSelected = currentDestination == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title
                                    )
                                },
                                label = { Text(text = destination.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = VlastTokens.BrandCyan,
                                    selectedTextColor = VlastTokens.BrandCyan,
                                    unselectedIconColor = VlastTokens.TextMuted,
                                    unselectedTextColor = VlastTokens.TextMuted,
                                    indicatorColor = VlastTokens.BrandCyan.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(VlastTokens.DarkBackground)
                ) {
                    when (currentDestination) {
                        VlastNavDestination.HOME -> {
                            DashboardScreen(
                                todayRecord = todayRecord,
                                activeNetwork = activeNetwork,
                                simSlot = settings.activeSimSlot,
                                priorityStatus = priorityStatus,
                                isKillSwitchActive = settings.killSwitchActive,
                                undoState = undoState,
                                meterStatus = viewModel.getMeterStatus(activeNetwork),
                                isFirstDayEmpty = viewModel.isFirstDayEmptyState(),
                                onToggleKillSwitch = {
                                    viewModel.requestKillSwitchToggle(!settings.killSwitchActive)
                                },
                                onOpenEditRecurringLimit = {
                                    editDialogTarget = activeNetwork
                                    isEditingTodayOverride = false
                                },
                                onToggleRecurringEnabled = { enabled ->
                                    val currentLimit = when (activeNetwork) {
                                        NetworkType.WIFI -> todayRecord.wifiRecurringLimitBytes
                                        NetworkType.MOBILE -> if (settings.activeSimSlot == 1) todayRecord.sim2RecurringLimitBytes else todayRecord.mobileRecurringLimitBytes
                                        NetworkType.NONE -> 0L
                                    }
                                    viewModel.requestSetRecurringLimit(
                                        networkType = activeNetwork,
                                        newLimitBytes = currentLimit,
                                        enabled = enabled,
                                        simSlot = settings.activeSimSlot
                                    )
                                },
                                onOpenSetTodayLimit = {
                                    editDialogTarget = activeNetwork
                                    isEditingTodayOverride = true
                                },
                                onClearTodayLimit = {
                                    viewModel.setTodayOverride(activeNetwork, null, settings.activeSimSlot)
                                },
                                onPerformUndo = { viewModel.performUndo() },
                                onCopyValue = { label, value ->
                                    viewModel.copyToClipboard(label, value)
                                },
                                onNavigateToReports = {
                                    currentDestination = VlastNavDestination.REPORTS
                                }
                            )
                        }
                        VlastNavDestination.REPORTS -> {
                            ReportsScreen(logs = activityLogs)
                        }
                        VlastNavDestination.HOTSPOT -> {
                            HotspotScreen(
                                hotspotUsedBytes = todayRecord.hotspotUsedBytes,
                                alertThresholdBytes = settings.hotspotAlertThresholdBytes,
                                isAlertEnabled = settings.hotspotAlertEnabled,
                                onConfigureThreshold = {
                                    isEditingHotspotThreshold = true
                                },
                                onToggleAlert = { enabled ->
                                    coroutineScope.launch {
                                        viewModel.repository.setHotspotAlertThreshold(
                                            settings.hotspotAlertThresholdBytes,
                                            enabled
                                        )
                                    }
                                }
                            )
                        }
                        VlastNavDestination.SETTINGS -> {
                            val sims = viewModel.dualSimManager.getAvailableSims()
                            SettingsScreen(
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { isDarkMode = it },
                                activeSimSlot = settings.activeSimSlot,
                                availableSims = sims,
                                onSelectSim = { slot -> viewModel.selectSimSlot(slot) },
                                isPinLockEnabled = settings.settingsLockEnabled,
                                onTogglePinLock = { enabled ->
                                    viewModel.setSettingsPinLock(enabled, if (enabled) "1234" else null)
                                },
                                isSoundEnabled = isCutSoundEnabled,
                                onToggleSound = { isCutSoundEnabled = it }
                            )
                        }
                    }
                }
            }

            // Sensitive Action Confirmation Dialog (Item 7)
            VlastConfirmationDialog(
                request = activeConfirmation,
                onConfirm = { viewModel.confirmAction() },
                onDismiss = { viewModel.dismissConfirmation() }
            )

            // Input Dialog for Limits
            if (editDialogTarget != null) {
                val targetNetwork = editDialogTarget!!
                val title = if (isEditingTodayOverride) {
                    "تحديد حد اليوم المؤقت لـ $targetNetwork"
                } else {
                    "تعديل الحد الدائم لـ $targetNetwork"
                }
                val currentVal = if (isEditingTodayOverride) {
                    todayRecord.getEffectiveLimit(targetNetwork, settings.activeSimSlot)
                } else {
                    when (targetNetwork) {
                        NetworkType.WIFI -> todayRecord.wifiRecurringLimitBytes
                        NetworkType.MOBILE -> if (settings.activeSimSlot == 1) todayRecord.sim2RecurringLimitBytes else todayRecord.mobileRecurringLimitBytes
                        NetworkType.NONE -> 0L
                    }
                }

                LimitInputDialog(
                    title = title,
                    initialBytes = currentVal,
                    onConfirm = { newBytes ->
                        if (isEditingTodayOverride) {
                            viewModel.setTodayOverride(targetNetwork, newBytes, settings.activeSimSlot)
                        } else {
                            viewModel.requestSetRecurringLimit(
                                networkType = targetNetwork,
                                newLimitBytes = newBytes,
                                enabled = true,
                                simSlot = settings.activeSimSlot
                            )
                        }
                        editDialogTarget = null
                    },
                    onDismiss = { editDialogTarget = null }
                )
            }

            // Hotspot Alert Input Dialog
            if (isEditingHotspotThreshold) {
                LimitInputDialog(
                    title = "عتبة تنبيه نقطة الاتصال",
                    initialBytes = settings.hotspotAlertThresholdBytes,
                    onConfirm = { bytes ->
                        coroutineScope.launch {
                            viewModel.repository.setHotspotAlertThreshold(bytes, true)
                        }
                        isEditingHotspotThreshold = false
                    },
                    onDismiss = { isEditingHotspotThreshold = false }
                )
            }
        }
    }
}
