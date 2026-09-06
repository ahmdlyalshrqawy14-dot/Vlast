package com.example.core.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.core.database.VlastDatabase
import com.example.core.feedback.HapticFeedbackController
import com.example.core.model.ActivityLogEntry
import com.example.core.model.DailyUsageRecord
import com.example.core.model.EnforcementDecision
import com.example.core.model.NetworkType
import com.example.core.model.SmartUnitFormatter
import com.example.core.network.NetworkStatsHelper
import com.example.core.network.NetworkTracker
import com.example.core.repository.UsageRepository
import com.example.core.rules.EnforcementRulesEngine
import com.example.core.widget.VlastAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Core local VPN service for Vlast (Phase 1 & Phase 2).
 *
 * Implements:
 * - Persistent notification with live remaining bandwidth and Kill Switch action button (Item 2)
 * - Clear, explicit cutoff notification naming specific reason (Item 3 & 10)
 * - Haptic vibration alerts at 90% and cutoff moment (Item 15)
 * - Activity log entry for every cutoff and restoration (Item 11)
 * - Dual SIM support (Item 13)
 * - Debounce protection (Item 18)
 * - Home widget updates (Item 8)
 */
class VlastVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetLoopJob: Job? = null
    private var periodicCalibrationJob: Job? = null

    private lateinit var usageRepository: UsageRepository
    private lateinit var networkTracker: NetworkTracker
    private lateinit var networkStatsHelper: NetworkStatsHelper
    private lateinit var hapticController: HapticFeedbackController

    @Volatile
    private var currentRecord: DailyUsageRecord? = null

    @Volatile
    private var activeNetworkType: NetworkType = NetworkType.NONE

    @Volatile
    private var killSwitchOverride: Boolean = false

    @Volatile
    private var activeSimSlot: Int = 0

    // Prevent duplicate cutoff notifications/vibrations in a tight loop
    private var lastCutoffNotificationKey: String? = null
    private var hasTriggered90PercentWarning = false

    override fun onCreate() {
        super.onCreate()
        val database = VlastDatabase.getInstance(applicationContext)
        usageRepository = UsageRepository(
            dailyUsageDao = database.dailyUsageDao(),
            appSettingsDao = database.appSettingsDao(),
            activityLogDao = database.activityLogDao()
        )
        networkTracker = NetworkTracker(applicationContext)
        networkStatsHelper = NetworkStatsHelper(applicationContext)
        hapticController = HapticFeedbackController(applicationContext)

        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildPersistentNotification())

        networkTracker.startListening()

        // Observe active network transport
        serviceScope.launch {
            networkTracker.activeNetworkType.collectLatest { type ->
                activeNetworkType = type
                updateForegroundNotification()
                VlastAppWidgetProvider.notifyUpdate(applicationContext)
            }
        }

        // Observe daily record updates
        serviceScope.launch {
            usageRepository.observeTodayRecord().collectLatest { record ->
                currentRecord = record
                check90PercentThreshold(record)
                updateForegroundNotification()
                VlastAppWidgetProvider.notifyUpdate(applicationContext)
            }
        }

        // Observe settings for kill switch & active SIM
        serviceScope.launch {
            usageRepository.observeSettings().collectLatest { settings ->
                val previousKillSwitch = killSwitchOverride
                killSwitchOverride = settings.killSwitchActive
                activeSimSlot = settings.activeSimSlot
                if (!killSwitchOverride && previousKillSwitch) {
                    // Reset cutoff key when kill switch is deactivated
                    lastCutoffNotificationKey = null
                }
                updateForegroundNotification()
                VlastAppWidgetProvider.notifyUpdate(applicationContext)
            }
        }

        startPeriodicCalibration()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP_VPN -> {
                stopVpnTunnel()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_KILL_SWITCH -> {
                serviceScope.launch {
                    val current = killSwitchOverride
                    usageRepository.setKillSwitch(!current)
                }
                return START_STICKY
            }
        }

        startVpnTunnel()
        return START_STICKY
    }

    @Synchronized
    private fun startVpnTunnel() {
        if (vpnInterface != null) return

        try {
            val builder = Builder()
                .setSession("VlastLocalEnforcer")
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)

            vpnInterface = builder.establish()

            serviceScope.launch {
                usageRepository.setMonitoringActive(true)
                usageRepository.logActivity(
                    eventType = ActivityLogEntry.EventType.RESTORED,
                    reason = ActivityLogEntry.SpecificCutReason.CONNECTION_RESTORED,
                    description = "تم بدء تشغيل نفق مراقبة وحماية استهلاك البيانات."
                )
            }

            startPacketLoop()
        } catch (e: Exception) {
            stopVpnTunnel()
        }
    }

    private fun startPacketLoop() {
        packetLoopJob?.cancel()
        packetLoopJob = serviceScope.launch {
            val pfd = vpnInterface ?: return@launch
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val outputStream = FileOutputStream(pfd.fileDescriptor)
            val packetBuffer = ByteBuffer.allocate(32767)
            val rawBytes = ByteArray(32767)

            var accumulatedBatchBytes = 0L
            var lastBatchFlushTimestamp = System.currentTimeMillis()

            while (isActive) {
                packetBuffer.clear()
                val bytesRead = inputStream.read(packetBuffer.array())
                if (bytesRead > 0) {
                    System.arraycopy(packetBuffer.array(), 0, rawBytes, 0, bytesRead)
                    val record = currentRecord ?: usageRepository.getOrCreateTodayRecord()
                    val transport = activeNetworkType

                    // Real-time evaluation against business rules
                    val decision = EnforcementRulesEngine.evaluate(
                        record = record,
                        networkType = transport,
                        killSwitchOverride = killSwitchOverride
                    )

                    when (decision) {
                        is EnforcementDecision.Allowed -> {
                            accumulatedBatchBytes += bytesRead
                            val now = System.currentTimeMillis()
                            if (accumulatedBatchBytes >= 32768L || (now - lastBatchFlushTimestamp) >= 1000L) {
                                usageRepository.recordDeviceBytes(transport, accumulatedBatchBytes, activeSimSlot)
                                accumulatedBatchBytes = 0L
                                lastBatchFlushTimestamp = now
                            }
                        }
                        is EnforcementDecision.BlockedKillSwitch -> {
                            handleCutoffEvent(
                                key = "KILL_SWITCH",
                                title = "تم القطع الكامل اليدوي",
                                reason = ActivityLogEntry.SpecificCutReason.MANUAL_KILL_SWITCH,
                                message = "تم قطع الاتصال يدويًا عبر ميزة القطع الكامل."
                            )
                            val rstPacket = EnforcementRulesEngine.generateCleanTerminationPacket(rawBytes, bytesRead)
                            if (rstPacket != null) {
                                try {
                                    outputStream.write(rstPacket)
                                } catch (_: Exception) {}
                            }
                        }
                        is EnforcementDecision.BlockedLimitExceeded -> {
                            val (key, title, reason, message) = when (transport) {
                                NetworkType.WIFI -> {
                                    if (record.wifiTodayOverrideLimitBytes != null) {
                                        Quadruple(
                                            "WIFI_TODAY",
                                            "خلص حد الواي فاي اليومي",
                                            ActivityLogEntry.SpecificCutReason.WIFI_TODAY_LIMIT,
                                            "تم استهلاك الحد المؤقت لليوم على شبكة Wi-Fi."
                                        )
                                    } else {
                                        Quadruple(
                                            "WIFI_RECURRING",
                                            "خلص حد الواي فاي الدائم",
                                            ActivityLogEntry.SpecificCutReason.WIFI_RECURRING_LIMIT,
                                            "تم استهلاك الحد اليومي الدائم على شبكة Wi-Fi."
                                        )
                                    }
                                }
                                NetworkType.MOBILE -> {
                                    if (activeSimSlot == 1) {
                                        Quadruple(
                                            "MOBILE_SIM2",
                                            "خلص حد الشريحة الثانية (SIM 2)",
                                            ActivityLogEntry.SpecificCutReason.MOBILE_SIM2_LIMIT,
                                            "تم استهلاك حد الشريحة الثانية (SIM 2)."
                                        )
                                    } else if (record.mobileTodayOverrideLimitBytes != null) {
                                        Quadruple(
                                            "MOBILE_TODAY",
                                            "خلص حد الموبايل اليومي",
                                            ActivityLogEntry.SpecificCutReason.MOBILE_TODAY_LIMIT,
                                            "تم استهلاك الحد المؤقت لليوم على بيانات الهاتف."
                                        )
                                    } else {
                                        Quadruple(
                                            "MOBILE_RECURRING",
                                            "خلص حد الموبايل الدائم",
                                            ActivityLogEntry.SpecificCutReason.MOBILE_RECURRING_LIMIT,
                                            "تم استهلاك الحد اليومي الدائم لبيانات الهاتف."
                                        )
                                    }
                                }
                                NetworkType.NONE -> Quadruple(
                                    "UNKNOWN",
                                    "توقف الاتصال",
                                    ActivityLogEntry.SpecificCutReason.MANUAL_KILL_SWITCH,
                                    "توقف تدفق البيانات."
                                )
                            }

                            handleCutoffEvent(key, title, reason, message)

                            val rstPacket = EnforcementRulesEngine.generateCleanTerminationPacket(rawBytes, bytesRead)
                            if (rstPacket != null) {
                                try {
                                    outputStream.write(rstPacket)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                } else if (bytesRead < 0) {
                    break
                }
            }

            if (accumulatedBatchBytes > 0L) {
                usageRepository.recordDeviceBytes(activeNetworkType, accumulatedBatchBytes, activeSimSlot)
            }
        }
    }

    private data class Quadruple(
        val key: String,
        val title: String,
        val reason: ActivityLogEntry.SpecificCutReason,
        val message: String
    )

    private fun handleCutoffEvent(
        key: String,
        title: String,
        reason: ActivityLogEntry.SpecificCutReason,
        message: String
    ) {
        if (lastCutoffNotificationKey == key) return
        lastCutoffNotificationKey = key

        // Item 15: Haptic feedback at cutoff moment
        hapticController.triggerCutoffHaptic()

        // Item 3 & 10: Explicit clear cutoff notification with specific reason
        val notificationManager = getSystemService(NotificationManager::class.java)
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CUTOFF_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager?.notify(CUTOFF_NOTIFICATION_ID, notification)

        // Item 11: Activity Log entry
        serviceScope.launch {
            usageRepository.logActivity(
                eventType = ActivityLogEntry.EventType.DISCONNECTED,
                reason = reason,
                description = "$title: $message"
            )
        }
    }

    private fun check90PercentThreshold(record: DailyUsageRecord) {
        val limit = record.getEffectiveLimit(activeNetworkType, activeSimSlot) ?: return
        val used = record.getUsedBytes(activeNetworkType, activeSimSlot)
        if (limit <= 0L) return

        val ratio = used.toDouble() / limit.toDouble()
        if (ratio >= 0.90 && ratio < 1.0) {
            if (!hasTriggered90PercentWarning) {
                hasTriggered90PercentWarning = true
                hapticController.triggerWarning90Percent()
            }
        } else if (ratio < 0.70) {
            hasTriggered90PercentWarning = false
        }
    }

    private fun startPeriodicCalibration() {
        periodicCalibrationJob?.cancel()
        periodicCalibrationJob = serviceScope.launch {
            while (isActive) {
                delay(15 * 60 * 1000L) // Every 15 minutes
                try {
                    val wifiBaseline = networkStatsHelper.queryWifiDeviceBytesToday()
                    val mobileBaseline = networkStatsHelper.queryMobileDeviceBytesToday()
                    if (wifiBaseline > 0L || mobileBaseline > 0L) {
                        usageRepository.calibrateUsage(wifiBaseline, mobileBaseline)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    @Synchronized
    private fun stopVpnTunnel() {
        packetLoopJob?.cancel()
        packetLoopJob = null
        periodicCalibrationJob?.cancel()
        periodicCalibrationJob = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        } finally {
            vpnInterface = null
        }

        serviceScope.launch {
            usageRepository.setMonitoringActive(false)
            usageRepository.logActivity(
                eventType = ActivityLogEntry.EventType.DISCONNECTED,
                reason = ActivityLogEntry.SpecificCutReason.MANUAL_KILL_SWITCH,
                description = "تم إيقاف خدمة مراقبة البيانات."
            )
        }
    }

    override fun onDestroy() {
        networkTracker.stopListening()
        stopVpnTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpnTunnel()
        stopSelf()
        super.onRevoke()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Ongoing monitoring channel
            val monitorChannel = NotificationChannel(
                CHANNEL_ID,
                "مراقبة استهلاك Vlast",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار دائم يعرض الاستهلاك المتبقي لحظيًا"
                setShowBadge(false)
            }
            manager?.createNotificationChannel(monitorChannel)

            // Critical cutoff alert channel
            val cutoffChannel = NotificationChannel(
                CUTOFF_CHANNEL_ID,
                "تنبيهات قطع البيانات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعار فوري عند الوصول للحدود أو تفعيل القطع الكامل"
                enableVibration(true)
            }
            manager?.createNotificationChannel(cutoffChannel)
        }
    }

    private fun updateForegroundNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, buildPersistentNotification())
    }

    /**
     * Item 2: Persistent Notification showing live remaining bandwidth
     * and a fast action button to toggle Kill Switch directly from notification.
     */
    private fun buildPersistentNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingContentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Kill Switch toggle action intent
        val toggleKillSwitchIntent = Intent(this, VlastVpnService::class.java).apply {
            action = ACTION_TOGGLE_KILL_SWITCH
        }
        val pendingToggleIntent = PendingIntent.getService(
            this,
            2,
            toggleKillSwitchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val record = currentRecord
        val remainingBytes = record?.getRemainingBytes(activeNetworkType, activeSimSlot)
        val networkName = when (activeNetworkType) {
            NetworkType.WIFI -> "Wi-Fi"
            NetworkType.MOBILE -> if (activeSimSlot == 1) "موبايل (SIM 2)" else "موبايل"
            NetworkType.NONE -> "لا يوجد اتصال"
        }

        val remainingText = if (remainingBytes != null) {
            "المتبقي: ${SmartUnitFormatter.formatDisplay(remainingBytes)} ($networkName)"
        } else {
            "الاستهلاك حر بدون حد ($networkName)"
        }

        val title = if (killSwitchOverride) {
            "القطع الكامل: مفعل (البيانات متوقفة)"
        } else {
            "Vlast: $remainingText"
        }

        val actionTitle = if (killSwitchOverride) "استئناف البيانات" else "قطع كامل الآن"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("اضغط لإدارة الحدود أو استخدام المفتاح السريع بالأسفل")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingContentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, actionTitle, pendingToggleIntent)
            .build()
    }

    companion object {
        const val ACTION_START_VPN = "com.example.core.vpn.START"
        const val ACTION_STOP_VPN = "com.example.core.vpn.STOP"
        const val ACTION_TOGGLE_KILL_SWITCH = "com.example.core.vpn.TOGGLE_KILL_SWITCH"

        private const val NOTIFICATION_ID = 101
        private const val CUTOFF_NOTIFICATION_ID = 102
        private const val CHANNEL_ID = "vlast_monitoring_channel"
        private const val CUTOFF_CHANNEL_ID = "vlast_cutoff_channel"
    }
}
