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
import com.example.core.viewmodel.VlastMechanicsViewModel
import com.example.ui.components.AppControlDashboardCard
import com.example.ui.components.CutoffActiveBanner
import com.example.ui.components.DynamicIslandHeader
import com.example.ui.components.FirstDayEmptyStateCard
import com.example.ui.components.HeroRemainingDisplay
import com.example.ui.components.KillSwitchCard
import com.example.ui.components.RapidUndoSnackbar
import com.example.ui.components.RecurringLimitCard
import com.example.ui.components.ServicePriorityBanner
import com.example.ui.components.TodayOverrideCard
import com.example.ui.theme.VlastTokens

/**
 * Screen 1: Primary Dashboard (Items 1, 4, 9, 11, 12, 15, 16, 19, 21):
 * - Dynamic-Island animated top bar
 * - Hero Remaining Counter (largest element)
 * - Service Priority Resolution Banner
 * - 3 Distinct Service Cards (Recurring, Today, Kill Switch)
 * - Rapid Undo window & Cutoff alerts
 * - Adaptive layout support for Foldables/Tablets (Item 15)
 */
@Composable
fun DashboardScreen(
    todayRecord: DailyUsageRecord,
    activeNetwork: NetworkType,
    simSlot: Int,
    priorityStatus: ServicePriorityStatus,
    isKillSwitchActive: Boolean,
    undoState: VlastMechanicsViewModel.UndoState?,
    meterStatus: MeterProgressStatus,
    isFirstDayEmpty: Boolean,
    onToggleKillSwitch: () -> Unit,
    onOpenEditRecurringLimit: () -> Unit,
    onToggleRecurringEnabled: (Boolean) -> Unit,
    onOpenSetTodayLimit: () -> Unit,
    onClearTodayLimit: () -> Unit,
    onPerformUndo: () -> Unit,
    onCopyValue: (String, String) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToAppControl: () -> Unit = {},
    activeAppRulesCount: Int = 0,
    modifier: Modifier = Modifier,
    isColorBlindMode: Boolean = false
) {
    val remainingBytes = todayRecord.getRemainingBytes(activeNetwork, simSlot)
    val usedBytes = todayRecord.getUsedBytes(activeNetwork, simSlot)
    val effectiveLimit = todayRecord.getEffectiveLimit(activeNetwork, simSlot)

    val networkLabel = when (activeNetwork) {
        NetworkType.WIFI -> "شبكة Wi-Fi"
        NetworkType.MOBILE -> if (simSlot == 1) "موبايل (SIM 2)" else "بيانات الهاتف (SIM 1)"
        NetworkType.NONE -> "لا يوجد اتصال حاليًا"
    }

    val recurringLimit = when (activeNetwork) {
        NetworkType.WIFI -> todayRecord.wifiRecurringLimitBytes
        NetworkType.MOBILE -> if (simSlot == 1) todayRecord.sim2RecurringLimitBytes else todayRecord.mobileRecurringLimitBytes
        NetworkType.NONE -> 0L
    }
    val isRecurringEnabled = when (activeNetwork) {
        NetworkType.WIFI -> todayRecord.wifiRecurringEnabled
        NetworkType.MOBILE -> if (simSlot == 1) todayRecord.sim2RecurringEnabled else todayRecord.mobileRecurringEnabled
        NetworkType.NONE -> false
    }

    val todayOverride = when (activeNetwork) {
        NetworkType.WIFI -> todayRecord.wifiTodayOverrideLimitBytes
        NetworkType.MOBILE -> if (simSlot == 1) todayRecord.sim2TodayOverrideLimitBytes else todayRecord.mobileTodayOverrideLimitBytes
        NetworkType.NONE -> null
    }

    val isLimitExceeded = todayRecord.isLimitReached(activeNetwork, simSlot)

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
            onNavigateToTodayOverride = onOpenSetTodayLimit,
            onNavigateToReports = onNavigateToReports
        )

        // 2. Cutoff/Lock Banner if connection is stopped (Item 19)
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

        // 3. Service Priority Status Banner (Item 4)
        ServicePriorityBanner(status = priorityStatus)

        // 4. Hero Counter Display (Item 11: Largest visual element)
        HeroRemainingDisplay(
            networkLabel = networkLabel,
            remainingBytes = remainingBytes,
            usedBytes = usedBytes,
            effectiveLimitBytes = effectiveLimit,
            status = meterStatus,
            onCopyValue = onCopyValue,
            isColorBlindMode = isColorBlindMode
        )

        // 5. First-day Empty State if zero consumption (Item 21)
        if (isFirstDayEmpty) {
            FirstDayEmptyStateCard(onConfigureLimits = onOpenEditRecurringLimit)
        }

        // 6. Rapid Undo Window (Item 9)
        RapidUndoSnackbar(
            undoState = undoState,
            onUndoClick = onPerformUndo
        )

        // 7. Three Distinct Service Cards (Items 9 & 12)
        RecurringLimitCard(
            currentLimitFormatted = SmartUnitFormatter.formatDisplay(recurringLimit),
            isEnabled = isRecurringEnabled,
            onEditLimit = onOpenEditRecurringLimit,
            onToggleEnabled = onToggleRecurringEnabled,
            isOverriddenByToday = todayOverride != null && todayOverride > 0L
        )

        TodayOverrideCard(
            overrideFormatted = if (todayOverride != null) SmartUnitFormatter.formatDisplay(todayOverride) else null,
            isActive = todayOverride != null && todayOverride > 0L,
            onSetTodayLimit = onOpenSetTodayLimit,
            onClearTodayLimit = onClearTodayLimit
        )

        KillSwitchCard(
            isActive = isKillSwitchActive,
            onToggle = { onToggleKillSwitch() }
        )

        // 8. Per-App Control Card on Dashboard (Item 4)
        AppControlDashboardCard(
            activeRulesCount = activeAppRulesCount,
            onNavigateToAppControl = onNavigateToAppControl
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
