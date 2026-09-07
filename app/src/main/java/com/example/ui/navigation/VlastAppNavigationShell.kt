package com.example.ui.navigation

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.model.NetworkType
import com.example.core.sim.DualSimManager
import com.example.core.viewmodel.VlastMechanicsViewModel
import com.example.ui.components.VlastConfirmationDialog
import com.example.ui.dialogs.LimitInputDialog
import com.example.ui.dialogs.KillSwitchSignatureMomentDialog
import com.example.ui.dialogs.VlastBrandedErrorDialog
import com.example.ui.screens.AppControlScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HotspotScreen
import com.example.ui.screens.PlayStorePreviewScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.VlastTheme
import com.example.ui.theme.VlastTokens
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main Application Navigation & Screen Shell:
 *
 * Implements:
 * - Native RTL Layout (Item 14)
 * - Responsive Layout (Section 10): NavigationBar (Compact) vs NavigationRail (Medium/Expanded)
 * - FLAG_SECURE on PIN lock prompt (Section 7)
 * - Historical Reports sub-tabs & 7/30 days charts (Section 3)
 * - Integrated Settings with sound, color-blind mode, and permission handling (Sections 2, 8, 11)
 * - Shortcuts & Dynamic language switching (Sections 4, 6)
 */

enum class VlastNavDestination(val titleRes: Int, val icon: ImageVector) {
    HOME(R.string.nav_home, Icons.Filled.Home),
    REPORTS(R.string.nav_reports, Icons.Filled.BarChart),
    HOTSPOT(R.string.nav_hotspot, Icons.Filled.WifiTethering),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings)
}

@Composable
fun VlastAppNavigationShell(
    viewModel: VlastMechanicsViewModel,
    modifier: Modifier = Modifier,
    onLanguageChange: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(VlastNavDestination.HOME) }
    var isDarkMode by remember { mutableStateOf(true) } // Item 2: Dark mode default

    val todayRecord by viewModel.todayRecord.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activeNetwork by viewModel.activeNetwork.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val priorityStatus by viewModel.currentPriorityStatus.collectAsState()
    val undoState by viewModel.undoState.collectAsState()
    val activeConfirmation by viewModel.activeConfirmation.collectAsState()
    val copyMessage by viewModel.copyMessage.collectAsState()
    val historicalRecords by viewModel.historicalRecords.collectAsState()
    val historyRangeDays by viewModel.historyDays.collectAsState()
    val isSettingsUnlocked by viewModel.isSettingsUnlocked.collectAsState()
    val appRules by viewModel.appRules.collectAsState()
    val selectedDashboardTab by viewModel.selectedDashboardTab.collectAsState()
    val showVpnErrorDialog by viewModel.showVpnErrorDialog.collectAsState()
    val showKillSwitchSignatureMoment by viewModel.showKillSwitchSignatureMoment.collectAsState()

    var isStorePreviewOpen by remember { mutableStateOf(false) }

    var availableSims by remember { mutableStateOf<List<DualSimManager.SimSlotInfo>>(emptyList()) }
    LaunchedEffect(Unit) {
        availableSims = viewModel.dualSimManager.getAvailableSims()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(copyMessage) {
        copyMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Phase 4: Per-App Control UI state
    var isAppControlOpen by remember { mutableStateOf(false) }
    var editingAppLimitPackage by remember { mutableStateOf<String?>(null) }
    var editingAppLimitName by remember { mutableStateOf("") }
    var editingAppLimitInitialBytes by remember { mutableStateOf<Long?>(null) }

    // Dialog state
    var editDialogTarget by remember { mutableStateOf<NetworkType?>(null) }
    var isEditingTodayOverride by remember { mutableStateOf(false) }
    var isEditingHotspotThreshold by remember { mutableStateOf(false) }

    // PIN Lock Dialog state (Section 7)
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInputValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Handle destination change with PIN protection
    fun navigateTo(destination: VlastNavDestination) {
        isAppControlOpen = false
        if (destination == VlastNavDestination.SETTINGS && settings.settingsLockEnabled && !isSettingsUnlocked) {
            pinInputValue = ""
            pinError = false
            showPinDialog = true
        } else {
            currentDestination = destination
        }
    }

    // Determine current language tag
    val currentLang = remember {
        val defaultLocale = Locale.getDefault().language
        if (defaultLocale == "en") "en" else "ar"
    }

    // Native Right-to-Left (RTL) Layout Provider (Item 14)
    val layoutDirection = if (currentLang == "en") LayoutDirection.Ltr else LayoutDirection.Rtl
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        VlastTheme(darkTheme = isDarkMode) {
            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 600.dp

                if (isWideScreen) {
                    // Wide Screen / Tablet Layout (Section 10): NavigationRail + Centered Content
                    Row(modifier = Modifier.fillMaxSize().background(VlastTokens.DarkBackground)) {
                        NavigationRail(
                            containerColor = VlastTokens.DarkSurface,
                            contentColor = VlastTokens.TextPrimary,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(Modifier.height(16.dp))
                            VlastNavDestination.values().forEach { destination ->
                                val isSelected = currentDestination == destination
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { navigateTo(destination) },
                                    icon = {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = stringResource(destination.titleRes)
                                        )
                                    },
                                    label = { Text(text = stringResource(destination.titleRes), fontSize = 11.sp) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = VlastTokens.BrandCyan,
                                        selectedTextColor = VlastTokens.BrandCyan,
                                        unselectedIconColor = VlastTokens.TextMuted,
                                        unselectedTextColor = VlastTokens.TextMuted,
                                        indicatorColor = VlastTokens.BrandCyan.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(VlastTokens.DarkBackground)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 700.dp)
                                    .align(Alignment.Center)
                            ) {
                                if (isStorePreviewOpen) {
                                    PlayStorePreviewScreen(onBack = { isStorePreviewOpen = false })
                                } else if (isAppControlOpen) {
                                    AppControlScreen(
                                        appRules = appRules,
                                        onRequestBlockToggle = { pkg, name, targetBlocked ->
                                            viewModel.requestAppBlockToggle(pkg, name, targetBlocked)
                                        },
                                        onOpenSetAppLimit = { pkg, name, currentLimit ->
                                            editingAppLimitPackage = pkg
                                            editingAppLimitName = name
                                            editingAppLimitInitialBytes = currentLimit
                                        },
                                        onDisableAppLimit = { pkg, name ->
                                            viewModel.setAppDailyLimit(pkg, name, null, false)
                                        },
                                        onUpdateAppNetworkTargets = { pkg, name, wifi, sim1, sim2 ->
                                            viewModel.setAppNetworkTargets(pkg, name, wifi, sim1, sim2)
                                        },
                                        onBack = { isAppControlOpen = false }
                                    )
                                } else {
                                    AppContent(
                                        currentDestination = currentDestination,
                                        viewModel = viewModel,
                                        todayRecord = todayRecord,
                                        settings = settings,
                                        activeNetwork = activeNetwork,
                                        activityLogs = activityLogs,
                                        historicalRecords = historicalRecords,
                                        historyRangeDays = historyRangeDays,
                                        priorityStatus = priorityStatus,
                                        undoState = undoState,
                                        isDarkMode = isDarkMode,
                                        onToggleDarkMode = { isDarkMode = it },
                                        availableSims = availableSims,
                                        onRefreshSims = { availableSims = viewModel.dualSimManager.getAvailableSims() },
                                        currentLanguage = currentLang,
                                        onLanguageChange = onLanguageChange,
                                        onOpenEditRecurringLimit = { network ->
                                            editDialogTarget = network
                                            isEditingTodayOverride = false
                                        },
                                        onOpenSetTodayLimit = { network ->
                                            editDialogTarget = network
                                            isEditingTodayOverride = true
                                        },
                                        onNavigateToReports = { currentDestination = VlastNavDestination.REPORTS },
                                        onConfigureHotspot = { isEditingHotspotThreshold = true },
                                        onNavigateToAppControl = { isAppControlOpen = true },
                                        onNavigateToStorePreview = { isStorePreviewOpen = true },
                                        appRules = appRules
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Mobile / Compact Layout: Scaffold + Bottom NavigationBar
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            NavigationBar(
                                containerColor = VlastTokens.DarkSurface,
                                contentColor = VlastTokens.TextPrimary
                            ) {
                                VlastNavDestination.values().forEach { destination ->
                                    val isSelected = currentDestination == destination
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { navigateTo(destination) },
                                        icon = {
                                            Icon(
                                                imageVector = destination.icon,
                                                contentDescription = stringResource(destination.titleRes)
                                            )
                                        },
                                        label = { Text(text = stringResource(destination.titleRes), fontSize = 11.sp) },
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
                            if (isStorePreviewOpen) {
                                PlayStorePreviewScreen(onBack = { isStorePreviewOpen = false })
                            } else if (isAppControlOpen) {
                                AppControlScreen(
                                    appRules = appRules,
                                    onRequestBlockToggle = { pkg, name, targetBlocked ->
                                        viewModel.requestAppBlockToggle(pkg, name, targetBlocked)
                                    },
                                    onOpenSetAppLimit = { pkg, name, currentLimit ->
                                        editingAppLimitPackage = pkg
                                        editingAppLimitName = name
                                        editingAppLimitInitialBytes = currentLimit
                                    },
                                    onDisableAppLimit = { pkg, name ->
                                        viewModel.setAppDailyLimit(pkg, name, null, false)
                                    },
                                    onUpdateAppNetworkTargets = { pkg, name, wifi, sim1, sim2 ->
                                        viewModel.setAppNetworkTargets(pkg, name, wifi, sim1, sim2)
                                    },
                                    onBack = { isAppControlOpen = false }
                                )
                            } else {
                                AppContent(
                                    currentDestination = currentDestination,
                                    viewModel = viewModel,
                                    todayRecord = todayRecord,
                                    settings = settings,
                                    activeNetwork = activeNetwork,
                                    activityLogs = activityLogs,
                                    historicalRecords = historicalRecords,
                                    historyRangeDays = historyRangeDays,
                                    priorityStatus = priorityStatus,
                                    undoState = undoState,
                                    isDarkMode = isDarkMode,
                                    onToggleDarkMode = { isDarkMode = it },
                                    availableSims = availableSims,
                                    onRefreshSims = { availableSims = viewModel.dualSimManager.getAvailableSims() },
                                    currentLanguage = currentLang,
                                    onLanguageChange = onLanguageChange,
                                    onOpenEditRecurringLimit = { network ->
                                        editDialogTarget = network
                                        isEditingTodayOverride = false
                                    },
                                    onOpenSetTodayLimit = { network ->
                                        editDialogTarget = network
                                        isEditingTodayOverride = true
                                    },
                                    onNavigateToReports = { currentDestination = VlastNavDestination.REPORTS },
                                    onConfigureHotspot = { isEditingHotspotThreshold = true },
                                    onNavigateToAppControl = { isAppControlOpen = true },
                                    onNavigateToStorePreview = { isStorePreviewOpen = true },
                                    appRules = appRules
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

                // Input Dialog for Hotspot Alert Threshold
                if (isEditingHotspotThreshold) {
                    LimitInputDialog(
                        title = "تحديد حد استهلاك نقطة الاتصال للتنبيه",
                        initialBytes = settings.hotspotAlertThresholdBytes,
                        onConfirm = { newBytes ->
                            coroutineScope.launch {
                                viewModel.repository.setHotspotAlertThreshold(
                                    newBytes,
                                    settings.hotspotAlertEnabled
                                )
                            }
                            isEditingHotspotThreshold = false
                        },
                        onDismiss = { isEditingHotspotThreshold = false }
                    )
                }

                // Phase 4: Input Dialog for Per-App Daily Limit
                if (editingAppLimitPackage != null) {
                    LimitInputDialog(
                        title = "تحديد الحد اليومي لتطبيق $editingAppLimitName",
                        initialBytes = editingAppLimitInitialBytes,
                        onConfirm = { newBytes ->
                            val pkg = editingAppLimitPackage!!
                            val name = editingAppLimitName
                            viewModel.setAppDailyLimit(pkg, name, newBytes, true)
                            editingAppLimitPackage = null
                        },
                        onDismiss = { editingAppLimitPackage = null }
                    )
                }

                // PIN Security Dialog with FLAG_SECURE (Section 7)
                if (showPinDialog) {
                    val activity = LocalContext.current as? Activity
                    DisposableEffect(Unit) {
                        activity?.window?.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                        onDispose {
                            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { showPinDialog = false },
                        containerColor = VlastTokens.DarkSurface,
                        icon = {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = VlastTokens.BrandAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.pin_prompt_title),
                                color = VlastTokens.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.pin_prompt_desc),
                                    color = VlastTokens.TextSecondary,
                                    fontSize = 12.sp
                                )
                                OutlinedTextField(
                                    value = pinInputValue,
                                    onValueChange = {
                                        if (it.length <= 8) {
                                            pinInputValue = it
                                            pinError = false
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (viewModel.unlockSettings(pinInputValue)) {
                                                showPinDialog = false
                                                currentDestination = VlastNavDestination.SETTINGS
                                            } else {
                                                pinError = true
                                            }
                                        }
                                    ),
                                    isError = pinError,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = VlastTokens.BrandAmber,
                                        unfocusedBorderColor = VlastTokens.DarkBorder,
                                        errorBorderColor = VlastTokens.SemanticRed
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (pinError) {
                                    Text(
                                        text = stringResource(R.string.pin_error),
                                        color = VlastTokens.SemanticRed,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (viewModel.unlockSettings(pinInputValue)) {
                                        showPinDialog = false
                                        currentDestination = VlastNavDestination.SETTINGS
                                    } else {
                                        pinError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VlastTokens.BrandAmber,
                                    contentColor = VlastTokens.DarkBackground
                                )
                            ) {
                                Text(stringResource(R.string.unlock_btn), fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showPinDialog = false },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VlastTokens.TextMuted)
                            ) {
                                Text(stringResource(R.string.cancel_btn))
                            }
                        }
                    )
                }

                // Items 5 & 14: First Run Explanation Dialog
                if (!settings.firstRunCompleted) {
                    com.example.ui.dialogs.FirstRunExplanationDialog(
                        onDismiss = {
                            viewModel.completeFirstRun()
                        }
                    )
                }

                // Item 22: Branded VPN Error Dialog
                if (showVpnErrorDialog) {
                    val context = LocalContext.current
                    VlastBrandedErrorDialog(
                        title = "تأمين حماية Vlast المحلي",
                        message = "يتطلب Vlast صلاحية إنشاء نفق فلترة شبكة محلي لحساب استهلاك الحزم وفرض حدود القطع تلقائياً دون إرسال أي بيانات لخوادم خارجية.",
                        onRetry = {
                            viewModel.triggerVpnErrorDialog(false)
                            (context as? com.example.MainActivity)?.checkAndStartVpn()
                        },
                        onDismiss = {
                            viewModel.triggerVpnErrorDialog(false)
                        }
                    )
                }

                // Item 28: Signature Moment Dialog for Emergency Kill Switch
                if (showKillSwitchSignatureMoment) {
                    KillSwitchSignatureMomentDialog(
                        onAcknowledge = {
                            viewModel.dismissKillSwitchSignatureMoment()
                        },
                        onUndo = {
                            viewModel.dismissKillSwitchSignatureMoment()
                            viewModel.requestKillSwitchToggle(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppContent(
    currentDestination: VlastNavDestination,
    viewModel: VlastMechanicsViewModel,
    todayRecord: com.example.core.model.DailyUsageRecord,
    settings: com.example.core.database.entity.AppSettingsEntity,
    activeNetwork: NetworkType,
    activityLogs: List<com.example.core.model.ActivityLogEntry>,
    historicalRecords: List<com.example.core.model.DailyUsageRecord>,
    historyRangeDays: Int,
    priorityStatus: com.example.core.model.ServicePriorityStatus,
    undoState: VlastMechanicsViewModel.UndoState?,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    availableSims: List<DualSimManager.SimSlotInfo>,
    onRefreshSims: () -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onOpenEditRecurringLimit: (NetworkType) -> Unit,
    onOpenSetTodayLimit: (NetworkType) -> Unit,
    onNavigateToReports: () -> Unit,
    onConfigureHotspot: () -> Unit,
    onNavigateToAppControl: () -> Unit = {},
    onNavigateToStorePreview: () -> Unit = {},
    appRules: List<com.example.core.model.ManagedAppRule> = emptyList()
) {
    val coroutineScope = rememberCoroutineScope()

    when (currentDestination) {
        VlastNavDestination.HOME -> {
            val selectedTab by viewModel.selectedDashboardTab.collectAsState()
            DashboardScreen(
                todayRecord = todayRecord,
                activeNetwork = activeNetwork,
                simSlot = settings.activeSimSlot,
                selectedTab = selectedTab,
                onSelectTab = { viewModel.selectDashboardTab(it) },
                priorityStatus = priorityStatus,
                isKillSwitchActive = settings.killSwitchActive,
                undoState = undoState,
                meterStatus = viewModel.getMeterStatus(activeNetwork),
                isFirstDayEmpty = viewModel.isFirstDayEmptyState(),
                isColorBlindMode = settings.colorBlindModeEnabled,
                onToggleKillSwitch = {
                    viewModel.requestKillSwitchToggle(!settings.killSwitchActive)
                },
                onOpenEditRecurringLimitForNetwork = { net, _ ->
                    onOpenEditRecurringLimit(net)
                },
                onToggleRecurringEnabledForNetwork = { net, slot, enabled ->
                    val currentLimit = when (net) {
                        NetworkType.WIFI -> todayRecord.wifiRecurringLimitBytes
                        NetworkType.MOBILE -> if (slot == 1) todayRecord.sim2RecurringLimitBytes else todayRecord.mobileRecurringLimitBytes
                        NetworkType.NONE -> 0L
                    }
                    viewModel.requestSetRecurringLimit(
                        networkType = net,
                        newLimitBytes = currentLimit,
                        enabled = enabled,
                        simSlot = slot
                    )
                },
                onOpenSetTodayLimitForNetwork = { net, _ ->
                    onOpenSetTodayLimit(net)
                },
                onClearTodayLimitForNetwork = { net, slot ->
                    viewModel.setTodayOverride(net, null, slot)
                },
                onPerformUndo = { viewModel.performUndo() },
                onCopyValue = { label, value ->
                    viewModel.copyToClipboard(label, value)
                },
                onNavigateToReports = onNavigateToReports,
                onNavigateToAppControl = onNavigateToAppControl,
                activeAppRulesCount = appRules.count { it.isFullyBlocked || (it.dailyLimitEnabled && (it.dailyLimitBytes ?: 0L) > 0L) }
            )
        }
        VlastNavDestination.REPORTS -> {
            ReportsScreen(
                logs = activityLogs,
                historicalRecords = historicalRecords,
                selectedRangeDays = historyRangeDays,
                onSelectRangeDays = { viewModel.setHistoryRange(it) }
            )
        }
        VlastNavDestination.HOTSPOT -> {
            HotspotScreen(
                hotspotUsedBytes = todayRecord.hotspotUsedBytes,
                alertThresholdBytes = settings.hotspotAlertThresholdBytes,
                isAlertEnabled = settings.hotspotAlertEnabled,
                onConfigureThreshold = onConfigureHotspot,
                onToggleAlert = { enabled ->
                    coroutineScope.launch {
                        viewModel.repository.setHotspotAlertThreshold(
                            settings.hotspotAlertThresholdBytes,
                            enabled
                        )
                    }
                },
                onManualRefresh = {
                    viewModel.refreshHotspotManually()
                }
            )
        }
        VlastNavDestination.SETTINGS -> {
            SettingsScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                activeSimSlot = settings.activeSimSlot,
                availableSims = availableSims,
                onSelectSim = { slot -> viewModel.selectSimSlot(slot) },
                onRefreshSims = onRefreshSims,
                isPinLockEnabled = settings.settingsLockEnabled,
                onTogglePinLock = { enabled ->
                    viewModel.setSettingsPinLock(enabled, if (enabled) "1234" else null)
                },
                isSoundEnabled = settings.soundAlertEnabled,
                onToggleSound = { viewModel.toggleSoundAlert(it) },
                isHapticEnabled = settings.hapticFeedbackEnabled,
                onToggleHaptic = { viewModel.toggleHapticFeedback(it) },
                isColorBlindMode = settings.colorBlindModeEnabled,
                onToggleColorBlindMode = { viewModel.toggleColorBlindMode(it) },
                currentLanguage = currentLanguage,
                onSelectLanguage = onLanguageChange,
                onNavigateToAppControl = onNavigateToAppControl,
                onNavigateToStorePreview = onNavigateToStorePreview,
                onRequestShutdownApp = { viewModel.requestShutdownApp() },
                onRequestFactoryResetApp = { viewModel.requestFactoryResetApp() }
            )
        }
    }
}
