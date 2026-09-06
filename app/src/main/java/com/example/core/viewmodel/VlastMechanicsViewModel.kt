package com.example.core.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.VlastDatabase
import com.example.core.database.entity.AppSettingsEntity
import com.example.core.model.ActivityLogEntry
import com.example.core.model.DailyUsageRecord
import com.example.core.model.MeterProgressStatus
import com.example.core.model.NetworkType
import com.example.core.model.ServicePriorityStatus
import com.example.core.model.SmartUnitFormatter
import com.example.core.network.NetworkStatsHelper
import com.example.core.network.NetworkTracker
import com.example.core.repository.UsageRepository
import com.example.core.model.HotspotState
import com.example.core.rules.HotspotAdaptiveEngine
import com.example.core.sim.DualSimManager
import com.example.core.vpn.VlastVpnService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Phase 2 Technical User Mechanics ViewModel.
 *
 * Implements:
 * - Live consumption counter with color gradations (Item 1)
 * - Prominent "Remaining" vs "Used" values (Item 1)
 * - Priority resolution display (Item 4)
 * - First-run explanation and VPN key clarification (Items 5 & 14)
 * - Smart unit converter integration (Item 6)
 * - Sensitive action confirmation handling (Item 7)
 * - Rapid undo timer for recurring limit reductions (Item 9)
 * - Activity log querying (Item 11)
 * - Long-press safety gate for critical actions (Item 12)
 * - Dual SIM slot switching (Item 13)
 * - Optional PIN lock for settings screen (Item 16)
 * - Fast clipboard copy (Item 21)
 * - First-day empty state detection (Item 22)
 * - Rapid-toggle debouncing (Item 18)
 */
class VlastMechanicsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VlastDatabase.getInstance(application)
    val repository = UsageRepository(
        dailyUsageDao = db.dailyUsageDao(),
        appSettingsDao = db.appSettingsDao(),
        activityLogDao = db.activityLogDao(),
        managedAppRuleDao = db.managedAppRuleDao()
    )
    private val networkTracker = NetworkTracker(application)
    val dualSimManager = DualSimManager(application)

    val activeNetwork: StateFlow<NetworkType> = networkTracker.activeNetworkType
    val todayRecord: StateFlow<DailyUsageRecord> = repository.observeTodayRecord()
        .stateIn(viewModelScope, SharingStarted.Eagerly, createInitialRecord())

    val settings: StateFlow<AppSettingsEntity> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettingsEntity())

    val activityLogs: StateFlow<List<ActivityLogEntry>> = repository.observeRecentLogs(50)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Phase 4: Per-App Control Rules
    val appRules: StateFlow<List<com.example.core.model.ManagedAppRule>> = repository.observeAllAppRules()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Section 3: Historical consumption charts
    private val _historyDays = MutableStateFlow(7)
    val historyDays: StateFlow<Int> = _historyDays.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val historicalRecords: StateFlow<List<DailyUsageRecord>> = _historyDays
        .flatMapLatest { days -> repository.observeRecentHistory(days) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setHistoryRange(days: Int) {
        _historyDays.value = days
    }

    // Undo state container for recurring limit changes (Item 9)
    data class UndoState(
        val networkType: NetworkType,
        val previousLimitBytes: Long,
        val previousEnabled: Boolean,
        val simSlot: Int,
        val secondsRemaining: Int
    )

    private val _undoState = MutableStateFlow<UndoState?>(null)
    val undoState: StateFlow<UndoState?> = _undoState.asStateFlow()
    private var undoJob: Job? = null

    // Sensitive action confirmation dialog state (Item 7)
    sealed class ConfirmationRequest {
        data object EnableKillSwitch : ConfirmationRequest()
        data class DecreaseRecurringLimit(
            val networkType: NetworkType,
            val newLimitBytes: Long,
            val simSlot: Int
        ) : ConfirmationRequest()
        data class EnableAppBlock(
            val packageName: String,
            val appName: String
        ) : ConfirmationRequest()
        data class DisableAppBlock(
            val packageName: String,
            val appName: String
        ) : ConfirmationRequest()
    }

    private val _activeConfirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val activeConfirmation: StateFlow<ConfirmationRequest?> = _activeConfirmation.asStateFlow()

    // Clipboard copy feedback message (Item 21)
    private val _copyMessage = MutableStateFlow<String?>(null)
    val copyMessage: StateFlow<String?> = _copyMessage.asStateFlow()

    // Item 22: Branded VPN Error Dialog State
    private val _showVpnErrorDialog = MutableStateFlow<Boolean>(false)
    val showVpnErrorDialog: StateFlow<Boolean> = _showVpnErrorDialog.asStateFlow()

    fun triggerVpnErrorDialog(show: Boolean) {
        _showVpnErrorDialog.value = show
    }

    // Item 28: Kill Switch Signature Moment State
    private val _showKillSwitchSignatureMoment = MutableStateFlow<Boolean>(false)
    val showKillSwitchSignatureMoment: StateFlow<Boolean> = _showKillSwitchSignatureMoment.asStateFlow()

    fun dismissKillSwitchSignatureMoment() {
        _showKillSwitchSignatureMoment.value = false
    }

    // Settings screen PIN lock authenticated state (Item 16)
    private val _isSettingsUnlocked = MutableStateFlow(false)
    val isSettingsUnlocked: StateFlow<Boolean> = _isSettingsUnlocked.asStateFlow()

    // Service priority resolution (Item 4)
    val currentPriorityStatus: StateFlow<ServicePriorityStatus> = combine(
        settings,
        todayRecord,
        activeNetwork
    ) { currentSettings, record, transport ->
        when {
            currentSettings.killSwitchActive || record.killSwitchEnabled -> {
                ServicePriorityStatus.KillSwitchInControl
            }
            transport == NetworkType.WIFI -> {
                if (record.wifiTodayOverrideLimitBytes != null && record.wifiTodayOverrideLimitBytes > 0L) {
                    ServicePriorityStatus.TodayOverrideInControl(
                        "Wi-Fi",
                        SmartUnitFormatter.formatDisplay(record.wifiTodayOverrideLimitBytes)
                    )
                } else if (record.wifiRecurringEnabled && record.wifiRecurringLimitBytes > 0L) {
                    ServicePriorityStatus.RecurringLimitInControl(
                        "Wi-Fi",
                        SmartUnitFormatter.formatDisplay(record.wifiRecurringLimitBytes)
                    )
                } else {
                    ServicePriorityStatus.UnlimitedInControl
                }
            }
            transport == NetworkType.MOBILE -> {
                val isSim2 = currentSettings.activeSimSlot == 1
                val networkLabel = if (isSim2) "موبايل SIM 2" else "موبايل"
                val override = if (isSim2) record.sim2TodayOverrideLimitBytes else record.mobileTodayOverrideLimitBytes
                val recurringEnabled = if (isSim2) record.sim2RecurringEnabled else record.mobileRecurringEnabled
                val recurringLimit = if (isSim2) record.sim2RecurringLimitBytes else record.mobileRecurringLimitBytes

                if (override != null && override > 0L) {
                    ServicePriorityStatus.TodayOverrideInControl(
                        networkLabel,
                        SmartUnitFormatter.formatDisplay(override)
                    )
                } else if (recurringEnabled && recurringLimit > 0L) {
                    ServicePriorityStatus.RecurringLimitInControl(
                        networkLabel,
                        SmartUnitFormatter.formatDisplay(recurringLimit)
                    )
                } else {
                    ServicePriorityStatus.UnlimitedInControl
                }
            }
            else -> ServicePriorityStatus.UnlimitedInControl
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ServicePriorityStatus.UnlimitedInControl)

    init {
        networkTracker.startListening()
        viewModelScope.launch {
            repository.getOrCreateTodayRecord()
        }
    }

    override fun onCleared() {
        networkTracker.stopListening()
        super.onCleared()
    }

    // --- Action Handlers ---

    /**
     * Toggles Kill Switch with confirmation check (Item 7 & 12).
     */
    fun requestKillSwitchToggle(targetState: Boolean) {
        if (targetState) {
            // Turning ON kill switch is sensitive -> Request confirmation
            _activeConfirmation.value = ConfirmationRequest.EnableKillSwitch
        } else {
            // Turning OFF does NOT require confirmation
            executeKillSwitch(false)
        }
    }

    fun confirmAction() {
        val request = _activeConfirmation.value
        _activeConfirmation.value = null
        when (request) {
            is ConfirmationRequest.EnableKillSwitch -> executeKillSwitch(true)
            is ConfirmationRequest.DecreaseRecurringLimit -> {
                executeSetRecurringLimit(
                    networkType = request.networkType,
                    newLimitBytes = request.newLimitBytes,
                    enabled = true,
                    simSlot = request.simSlot
                )
            }
            is ConfirmationRequest.EnableAppBlock -> {
                setAppFullyBlocked(request.packageName, request.appName, true)
            }
            is ConfirmationRequest.DisableAppBlock -> {
                setAppFullyBlocked(request.packageName, request.appName, false)
            }
            null -> {}
        }
    }

    fun requestAppBlockToggle(packageName: String, appName: String, targetBlocked: Boolean) {
        if (targetBlocked) {
            _activeConfirmation.value = ConfirmationRequest.EnableAppBlock(packageName, appName)
        } else {
            _activeConfirmation.value = ConfirmationRequest.DisableAppBlock(packageName, appName)
        }
    }

    fun dismissConfirmation() {
        _activeConfirmation.value = null
    }

    private fun executeKillSwitch(active: Boolean) {
        viewModelScope.launch {
            if (!repository.canToggleTunnel()) return@launch // Debounce protection (Item 18)
            repository.setKillSwitch(active)
            if (active) {
                _showKillSwitchSignatureMoment.value = true
            }
            ensureVpnServiceRunning()
        }
    }

    /**
     * Updates Recurring Limit with sensitivity check and Undo window (Item 7 & 9).
     */
    fun requestSetRecurringLimit(
        networkType: NetworkType,
        newLimitBytes: Long,
        enabled: Boolean,
        simSlot: Int = 0
    ) {
        val currentSettings = settings.value
        val currentLimit = when (networkType) {
            NetworkType.WIFI -> currentSettings.wifiRecurringLimitBytes
            NetworkType.MOBILE -> if (simSlot == 1) currentSettings.sim2RecurringLimitBytes else currentSettings.mobileRecurringLimitBytes
            NetworkType.NONE -> 0L
        }

        // Sensitivity rule: Only DECREASING the limit requires confirmation (Item 7)
        if (enabled && currentLimit > 0L && newLimitBytes < currentLimit) {
            _activeConfirmation.value = ConfirmationRequest.DecreaseRecurringLimit(
                networkType = networkType,
                newLimitBytes = newLimitBytes,
                simSlot = simSlot
            )
        } else {
            // Increasing or first-time setting -> Apply immediately with Undo option
            executeSetRecurringLimit(networkType, newLimitBytes, enabled, simSlot)
        }
    }

    private fun executeSetRecurringLimit(
        networkType: NetworkType,
        newLimitBytes: Long,
        enabled: Boolean,
        simSlot: Int
    ) {
        val currentSettings = settings.value
        val previousLimit = when (networkType) {
            NetworkType.WIFI -> currentSettings.wifiRecurringLimitBytes
            NetworkType.MOBILE -> if (simSlot == 1) currentSettings.sim2RecurringLimitBytes else currentSettings.mobileRecurringLimitBytes
            NetworkType.NONE -> 0L
        }
        val previousEnabled = when (networkType) {
            NetworkType.WIFI -> currentSettings.wifiRecurringEnabled
            NetworkType.MOBILE -> if (simSlot == 1) currentSettings.sim2RecurringEnabled else currentSettings.mobileRecurringEnabled
            NetworkType.NONE -> false
        }

        viewModelScope.launch {
            repository.setRecurringLimit(networkType, newLimitBytes, enabled, simSlot)
            repository.logActivity(
                eventType = ActivityLogEntry.EventType.CONFIG_CHANGED,
                reason = ActivityLogEntry.SpecificCutReason.LIMIT_EXPANDED,
                description = "تم تعديل الحد الدائم لـ $networkType إلى ${SmartUnitFormatter.formatDisplay(newLimitBytes)}"
            )

            // Launch 6-second rapid undo window (Item 9)
            launchUndoWindow(
                networkType = networkType,
                previousLimitBytes = previousLimit,
                previousEnabled = previousEnabled,
                simSlot = simSlot
            )
        }
    }

    private fun launchUndoWindow(
        networkType: NetworkType,
        previousLimitBytes: Long,
        previousEnabled: Boolean,
        simSlot: Int
    ) {
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            for (sec in 6 downTo 1) {
                _undoState.value = UndoState(
                    networkType = networkType,
                    previousLimitBytes = previousLimitBytes,
                    previousEnabled = previousEnabled,
                    simSlot = simSlot,
                    secondsRemaining = sec
                )
                delay(1000L)
            }
            _undoState.value = null
        }
    }

    fun performUndo() {
        val state = _undoState.value ?: return
        undoJob?.cancel()
        _undoState.value = null

        viewModelScope.launch {
            repository.setRecurringLimit(
                networkType = state.networkType,
                limitBytes = state.previousLimitBytes,
                enabled = state.previousEnabled,
                simSlot = state.simSlot
            )
            repository.logActivity(
                eventType = ActivityLogEntry.EventType.CONFIG_CHANGED,
                reason = ActivityLogEntry.SpecificCutReason.LIMIT_EXPANDED,
                description = "تم التراجع السريع عن تعديل الحد الدائم واستعادة القيمة السابقة."
            )
        }
    }

    /**
     * Today Temporary Override (Increasing today's limit does NOT require confirmation - Item 7).
     */
    fun setTodayOverride(
        networkType: NetworkType,
        overrideBytes: Long?,
        simSlot: Int = 0
    ) {
        viewModelScope.launch {
            repository.setTodayOverrideLimit(networkType, overrideBytes, simSlot)
            val display = if (overrideBytes != null) SmartUnitFormatter.formatDisplay(overrideBytes) else "إلغاء الحد المؤقت"
            repository.logActivity(
                eventType = ActivityLogEntry.EventType.CONFIG_CHANGED,
                reason = ActivityLogEntry.SpecificCutReason.LIMIT_EXPANDED,
                description = "تم تحديث الحد اليومي المؤقت لـ $networkType إلى $display"
            )
        }
    }

    /**
     * Active SIM Slot Switching (Item 13).
     */
    fun selectSimSlot(slot: Int) {
        viewModelScope.launch {
            repository.setActiveSimSlot(slot)
        }
    }

    /**
     * Quick Copy to Clipboard (Item 21).
     */
    fun copyToClipboard(label: String, value: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard?.setPrimaryClip(clip)

        viewModelScope.launch {
            _copyMessage.value = "تم نسخ $label بنجاح: $value"
            delay(2500L)
            _copyMessage.value = null
        }
    }

    /**
     * Settings Lock PIN verification (Item 16).
     */
    fun unlockSettings(pinEntered: String): Boolean {
        val savedPin = settings.value.settingsLockPin
        val isValid = savedPin == null || savedPin == pinEntered
        if (isValid) {
            _isSettingsUnlocked.value = true
        }
        return isValid
    }

    fun lockSettings() {
        _isSettingsUnlocked.value = false
    }

    fun setSettingsPinLock(enabled: Boolean, pin: String?) {
        viewModelScope.launch {
            repository.setSettingsLock(enabled, pin)
            _isSettingsUnlocked.value = !enabled
        }
    }

    private val _hotspotEngineState = MutableStateFlow(HotspotState())

    /**
     * Section 1: Manual refresh of hotspot consumption.
     */
    fun refreshHotspotManually() {
        viewModelScope.launch {
            val helper = NetworkStatsHelper(getApplication())
            val bytes = helper.queryHotspotBytesToday()
            val now = System.currentTimeMillis()
            val updated = HotspotAdaptiveEngine.onManualRefresh(
                currentState = _hotspotEngineState.value,
                manualBytes = bytes,
                timestamp = now
            )
            _hotspotEngineState.value = updated
            repository.updateHotspotBytes(bytes)
        }
    }

    /**
     * Section 2: Sound Alert toggle.
     */
    fun toggleSoundAlert(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundAlertEnabled(enabled)
        }
    }

    /**
     * Section 8: Color-blind mode toggle.
     */
    fun toggleColorBlindMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setColorBlindModeEnabled(enabled)
        }
    }

    // Phase 4: Per-App Control actions
    fun setAppFullyBlocked(packageName: String, appDisplayName: String, isBlocked: Boolean) {
        viewModelScope.launch {
            repository.setAppFullyBlocked(packageName, appDisplayName, isBlocked)
            if (isBlocked) {
                com.example.core.notification.AppControlNotificationHelper.notifyAppFullyBlocked(
                    getApplication(),
                    appDisplayName,
                    packageName
                )
            } else {
                com.example.core.notification.AppControlNotificationHelper.notifyAppUnblocked(
                    getApplication(),
                    appDisplayName,
                    packageName
                )
            }
        }
    }

    fun setAppDailyLimit(packageName: String, appDisplayName: String, limitBytes: Long?, enabled: Boolean) {
        viewModelScope.launch {
            repository.setAppDailyLimit(packageName, appDisplayName, limitBytes, enabled)
            if (enabled && limitBytes != null && limitBytes > 0L) {
                val limitStr = SmartUnitFormatter.formatDisplay(limitBytes)
                com.example.core.notification.AppControlNotificationHelper.notifyAppLimitUpdated(
                    getApplication(),
                    appDisplayName,
                    packageName,
                    limitStr
                )
            } else {
                com.example.core.notification.AppControlNotificationHelper.notifyAppLimitUpdated(
                    getApplication(),
                    appDisplayName,
                    packageName,
                    null
                )
            }
        }
    }

    fun deleteAppRule(packageName: String) {
        viewModelScope.launch {
            repository.deleteAppRule(packageName)
        }
    }

    /**
     * First-run walk-through completion (Item 5 & 14).
     */
    fun completeFirstRun() {
        viewModelScope.launch {
            repository.setFirstRunCompleted(true)
        }
    }

    /**
     * Ensures VPN service is running.
     */
    fun ensureVpnServiceRunning() {
        val context = getApplication<Application>()
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent == null) {
            val intent = Intent(context, VlastVpnService::class.java).apply {
                action = VlastVpnService.ACTION_START_VPN
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    /**
     * Evaluates empty state for first day (Item 22).
     */
    fun isFirstDayEmptyState(): Boolean {
        val record = todayRecord.value
        return record.wifiUsedBytes == 0L && record.mobileUsedBytes == 0L && record.hotspotUsedBytes == 0L
    }

    /**
     * Meter color status based on Item 1.
     */
    fun getMeterStatus(transport: NetworkType): MeterProgressStatus {
        val record = todayRecord.value
        val slot = settings.value.activeSimSlot
        val used = record.getUsedBytes(transport, slot)
        val limit = record.getEffectiveLimit(transport, slot)
        return MeterProgressStatus.fromUsage(used, limit)
    }

    private fun createInitialRecord(): DailyUsageRecord {
        return DailyUsageRecord(
            date = java.time.LocalDate.now().toString()
        )
    }
}
