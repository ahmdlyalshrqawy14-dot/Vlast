package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.model.DailyUsageRecord
import com.example.core.model.MeterProgressStatus
import com.example.core.model.NetworkType
import com.example.core.model.ServicePriorityStatus
import com.example.core.model.SmartUnitFormatter
import com.example.core.viewmodel.DashboardNetworkTab
import com.example.core.viewmodel.VlastMechanicsViewModel
import com.example.ui.components.AppControlDashboardCard
import com.example.ui.components.CutoffActiveBanner
import com.example.ui.components.DynamicIslandHeader
import com.example.ui.components.FirstDayEmptyStateCard
import com.example.ui.components.HeroRemainingDisplay
import com.example.ui.components.KillSwitchCard
import com.example.ui.components.NetworkTabsHeader
import com.example.ui.components.RapidUndoSnackbar
import com.example.ui.components.RecurringLimitCard
import com.example.ui.components.ServicePriorityBanner
import com.example.ui.components.TodayOverrideCard
import com.example.ui.theme.VlastTokens

/**
 * Screen 1: Primary Dashboard (Items 1, 4, 9, 11, 12, 15, 16, 19, 21):
 * - Dynamic-Island animated top bar
 * - 3 Functional Network Sections Header (Wi-Fi, SIM 1, SIM 2)
 * - Hero Remaining Counter (largest element) for the viewed network
 * - Service Priority Resolution Banner
 * - 3 Distinct Service Cards (Recurring, Today, Kill Switch) for the viewed network
 * - Rapid Undo window & Cutoff alerts
 * - Adaptive layout support for Foldables/Tablets
 */
@Composable
fun DashboardScreen(
    todayRecord: DailyUsageRecord,
    activeNetwork: NetworkType,
    simSlot: Int,
    selectedTab: DashboardNetworkTab = DashboardNetworkTab.WIFI,
    onSelectTab: (DashboardNetworkTab) -> Unit = {},
    priorityStatus: ServicePriorityStatus,
    isKillSwitchActive: Boolean,
    undoState: VlastMechanicsViewModel.UndoState?,
    meterStatus: MeterProgressStatus,
    isFirstDayEmpty: Boolean,
    onToggleKillSwitch: () -> Unit,
    onOpenEditRecurringLimitForNetwork: (NetworkType, Int) -> Unit = { _, _ -> },
    onToggleRecurringEnabledForNetwork: (NetworkType, Int, Boolean) -> Unit = { _, _, _ -> },
    onOpenSetTodayLimitForNetwork: (NetworkType, Int) -> Unit = { _, _ -> },
    onClearTodayLimitForNetwork: (NetworkType, Int) -> Unit = { _, _ -> },
    onPerformUndo: () -> Unit,
    onCopyValue: (String, String) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToAppControl: () -> Unit = {},
    activeAppRulesCount: Int = 0,
    modifier: Modifier = Modifier,
    isColorBlindMode: Boolean = false
) {
    // Determine the network being inspected on this tab
    val (viewNetwork, viewSimSlot) = when (selectedTab) {
        DashboardNetworkTab.WIFI -> NetworkType.WIFI to 0
        DashboardNetworkTab.SIM_1 -> NetworkType.MOBILE to 0
        DashboardNetworkTab.SIM_2 -> NetworkType.MOBILE to 1
    }

    val remainingBytes = todayRecord.getRemainingBytes(viewNetwork, viewSimSlot)
    val usedBytes = todayRecord.getUsedBytes(viewNetwork, viewSimSlot)
    val effectiveLimit = todayRecord.getEffectiveLimit(viewNetwork, viewSimSlot)

    val networkLabel = when (viewNetwork) {
        NetworkType.WIFI -> "شبكة Wi-Fi"
        NetworkType.MOBILE -> if (viewSimSlot == 1) "موبايل (SIM 2)" else "بيانات الهاتف (SIM 1)"
        NetworkType.NONE -> "لا يوجد اتصال حاليًا"
    }

    val recurringLimit = when (viewNetwork) {
        NetworkType.WIFI -> todayRecord.wifiRecurringLimitBytes
        NetworkType.MOBILE -> if (viewSimSlot == 1) todayRecord.sim2RecurringLimitBytes else todayRecord.mobileRecurringLimitBytes
        NetworkType.NONE -> 0L
    }
    val isRecurringEnabled = when (viewNetwork) {
        NetworkType.WIFI -> todayRecord.wifiRecurringEnabled
        NetworkType.MOBILE -> if (viewSimSlot == 1) todayRecord.sim2RecurringEnabled else todayRecord.mobileRecurringEnabled
        NetworkType.NONE -> false
    }

    val todayOverride = when (viewNetwork) {
        NetworkType.WIFI -> todayRecord.wifiTodayOverrideLimitBytes
        NetworkType.MOBILE -> if (viewSimSlot == 1) todayRecord.sim2TodayOverrideLimitBytes else todayRecord.mobileTodayOverrideLimitBytes
        NetworkType.NONE -> null
    }

    val isLimitExceeded = todayRecord.isLimitReached(viewNetwork, viewSimSlot)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VlastTokens.DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VlastTokens.Space16, vertical = VlastTokens.Space8),
        verticalArrangement = Arrangement.spacedBy(VlastTokens.Space16)
    ) {
        // 1. Dynamic Island Style Header (Item 16)
        DynamicIslandHeader(
            isKillSwitchActive = isKillSwitchActive,
            onToggleKillSwitch = onToggleKillSwitch,
            onNavigateToTodayOverride = { onOpenSetTodayLimitForNetwork(viewNetwork, viewSimSlot) },
            onNavigateToReports = onNavigateToReports
        )

        // 2. 3-Section Functional Network Tabs Header (Wi-Fi, SIM 1, SIM 2)
        NetworkTabsHeader(
            selectedTab = selectedTab,
            activeNetwork = activeNetwork,
            activeSimSlot = simSlot,
            onSelectTab = onSelectTab
        )

        // 3. Cutoff/Lock Banner if connection is stopped (Item 19)
        if (isKillSwitchActive || isLimitExceeded) {
            val reason = if (isKillSwitchActive) {
                "تم إيقاف البيانات يدويًا عبر مفتاح القطع الكامل (Kill Switch)."
            } else {
                "تم استهلاك كامل الحصة المحددة لهذا اليوم على $networkLabel."
            }
            CutoffActiveBanner(
                isKillSwitch = isKillSwitchActive,
                reasonText = reason,
                onQuickRestore = onToggleKillSwitch
            )
        }

        // 4. Service Priority Status Banner (Item 4)
        ServicePriorityBanner(status = priorityStatus)

        // 5. Hero Counter Display (Item 11: Largest visual element)
        HeroRemainingDisplay(
            networkLabel = networkLabel,
            remainingBytes = remainingBytes,
            usedBytes = usedBytes,
            effectiveLimitBytes = effectiveLimit,
            status = meterStatus,
            onCopyValue = onCopyValue,
            isColorBlindMode = isColorBlindMode
        )

        // 6. First-day Empty State if zero consumption (Item 21)
        if (isFirstDayEmpty) {
            FirstDayEmptyStateCard(onConfigureLimits = { onOpenEditRecurringLimitForNetwork(viewNetwork, viewSimSlot) })
        }

        // 7. Rapid Undo Window (Item 9)
        RapidUndoSnackbar(
            undoState = undoState,
            onUndoClick = onPerformUndo
        )

        // 8. Three Distinct Service Cards (Items 9 & 12)
        RecurringLimitCard(
            currentLimitFormatted = SmartUnitFormatter.formatDisplay(recurringLimit),
            isEnabled = isRecurringEnabled,
            onEditLimit = { onOpenEditRecurringLimitForNetwork(viewNetwork, viewSimSlot) },
            onToggleEnabled = { enabled -> onToggleRecurringEnabledForNetwork(viewNetwork, viewSimSlot, enabled) },
            isOverriddenByToday = todayOverride != null && todayOverride > 0L
        )

        TodayOverrideCard(
            overrideFormatted = if (todayOverride != null) SmartUnitFormatter.formatDisplay(todayOverride) else null,
            isActive = todayOverride != null && todayOverride > 0L,
            onSetTodayLimit = { onOpenSetTodayLimitForNetwork(viewNetwork, viewSimSlot) },
            onClearTodayLimit = { onClearTodayLimitForNetwork(viewNetwork, viewSimSlot) }
        )

        KillSwitchCard(
            isActive = isKillSwitchActive,
            onToggle = { onToggleKillSwitch() }
        )

        // 9. Per-App Control Card on Dashboard (Item 4)
        AppControlDashboardCard(
            activeRulesCount = activeAppRulesCount,
            onNavigateToAppControl = onNavigateToAppControl
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
