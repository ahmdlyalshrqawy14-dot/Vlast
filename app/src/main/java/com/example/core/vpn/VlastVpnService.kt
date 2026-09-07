package com.example.core.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.database.VlastDatabase
import com.example.core.feedback.HapticFeedbackController
import com.example.core.model.ActivityLogEntry
import com.example.core.model.DailyUsageRecord
import com.example.core.model.EnforcementDecision
import com.example.core.model.HotspotState
import com.example.core.model.ManagedAppRule
import com.example.core.model.NetworkType
import com.example.core.model.SmartUnitFormatter
import com.example.core.network.NetworkStatsHelper
import com.example.core.network.NetworkTracker
import com.example.core.notification.AppControlNotificationHelper
import com.example.core.repository.UsageRepository
import com.example.core.rules.EnforcementRulesEngine
import com.example.core.rules.HotspotAdaptiveEngine
import com.example.core.widget.VlastAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

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

    // Cutoff sound alert (Section 2)
    private var soundPool: SoundPool? = null
    private var cutoffSoundId: Int = 0
    private var isCutSoundEnabled: Boolean = true

    // Hotspot adaptive polling (Section 1)
    private var hotspotPollingJob: Job? = null
    private var hotspotState = HotspotState()

    // Phase 4: Per-App Control in-memory tracking
    private var connectivityManager: ConnectivityManager? = null
    private var isConnectionOwnerUidSupported = true
    private val managedAppRulesMap = ConcurrentHashMap<String, ManagedAppRule>()
    private val uidToPackageMap = ConcurrentHashMap<Int, String>()
    private val appBatchBytesMap = ConcurrentHashMap<String, Long>()

    // Prevent duplicate cutoff notifications/vibrations in a tight loop
    private var lastCutoffNotificationKey: String? = null
    private var hasTriggered90PercentWarning = false

    override fun onCreate() {
        super.onCreate()
        val database = VlastDatabase.getInstance(applicationContext)
        usageRepository = UsageRepository(
            dailyUsageDao = database.dailyUsageDao(),
            appSettingsDao = database.appSettingsDao(),
            activityLogDao = database.activityLogDao(),
            managedAppRuleDao = database.managedAppRuleDao()
        )
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkTracker = NetworkTracker(applicationContext)
        networkStatsHelper = NetworkStatsHelper(applicationContext)
        hapticController = HapticFeedbackController(applicationContext)

        // Initialize sound pool for cutoff sound alert (Section 2)
        try {
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttrs)
                .build().also { sp ->
                    cutoffSoundId = sp.load(applicationContext, R.raw.vlast_cutoff_tone, 1)
                }
        } catch (_: Exception) {}

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

        // Phase 4: Synchronize per-app rules in memory
        serviceScope.launch {
            usageRepository.observeAllAppRules().collectLatest { rules ->
                managedAppRulesMap.clear()
                rules.forEach { rule ->
                    managedAppRulesMap[rule.packageName] = rule
                }
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

        // Observe settings for kill switch, active SIM, and sound alert
        serviceScope.launch {
            usageRepository.observeSettings().collectLatest { settings ->
                val previousKillSwitch = killSwitchOverride
                killSwitchOverride = settings.killSwitchActive
                activeSimSlot = settings.activeSimSlot
                isCutSoundEnabled = settings.soundAlertEnabled
                if (!killSwitchOverride && previousKillSwitch) {
                    // Reset cutoff key when kill switch is deactivated
                    lastCutoffNotificationKey = null
                }
                updateForegroundNotification()
                VlastAppWidgetProvider.notifyUpdate(applicationContext)
            }
        }

        startPeriodicCalibration()
        startHotspotMonitoring()
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

            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    builder.setMetered(false)
                } catch (_: Exception) {}
            }

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

                    // Section 1: Determine packet owner UID on Android 10+ (API 29+)
                    var appRule: ManagedAppRule? = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && connectivityManager != null && isConnectionOwnerUidSupported) {
                        val version = (rawBytes[0].toInt() shr 4) and 0x0F
                        if (version == 4 && bytesRead >= 20) {
                            val protocol = rawBytes[9].toInt() and 0xFF
                            val ihl = (rawBytes[0].toInt() and 0x0F) * 4
                            if ((protocol == 6 || protocol == 17) && bytesRead >= ihl + 4) {
                                val srcIp = InetAddress.getByAddress(rawBytes.copyOfRange(12, 16))
                                val dstIp = InetAddress.getByAddress(rawBytes.copyOfRange(16, 20))
                                val srcPort = ((rawBytes[ihl].toInt() and 0xFF) shl 8) or (rawBytes[ihl + 1].toInt() and 0xFF)
                                val dstPort = ((rawBytes[ihl + 2].toInt() and 0xFF) shl 8) or (rawBytes[ihl + 3].toInt() and 0xFF)

                                val localSocket = InetSocketAddress(srcIp, srcPort)
                                val remoteSocket = InetSocketAddress(dstIp, dstPort)

                                val uid = try {
                                    val qUid = connectivityManager?.getConnectionOwnerUid(protocol, localSocket, remoteSocket) ?: -1
                                    if (qUid > 0) qUid else (connectivityManager?.getConnectionOwnerUid(protocol, remoteSocket, localSocket) ?: -1)
                                } catch (e: SecurityException) {
                                    isConnectionOwnerUidSupported = false
                                    -1
                                } catch (e: UnsupportedOperationException) {
                                    isConnectionOwnerUidSupported = false
                                    -1
                                } catch (_: Throwable) {
                                    -1
                                }

                                if (uid > 0) {
                                    val pkgName = uidToPackageMap.getOrPut(uid) {
                                        try {
                                            packageManager.getPackagesForUid(uid)?.firstOrNull() ?: ""
                                        } catch (_: Exception) {
                                            ""
                                        }
                                    }
                                    if (pkgName.isNotEmpty()) {
                                        appRule = managedAppRulesMap[pkgName]
                                    }
                                }
                            }
                        }
                    }

                    // Real-time evaluation against 4-tier business rules
                    val decision = EnforcementRulesEngine.evaluate(
                        record = record,
                        networkType = transport,
                        killSwitchOverride = killSwitchOverride,
                        appRule = appRule,
                        simSlot = activeSimSlot
                    )

                    when (decision) {
                        is EnforcementDecision.Allowed -> {
                            accumulatedBatchBytes += bytesRead
                            if (appRule != null) {
                                val prev = appBatchBytesMap[appRule.packageName] ?: 0L
                                appBatchBytesMap[appRule.packageName] = prev + bytesRead
                            }

                            val now = System.currentTimeMillis()
                            if (accumulatedBatchBytes >= 32768L || (now - lastBatchFlushTimestamp) >= 1000L) {
                                usageRepository.recordDeviceBytes(transport, accumulatedBatchBytes, activeSimSlot)
                                accumulatedBatchBytes = 0L

                                // Flush per-app batch bytes
                                if (appBatchBytesMap.isNotEmpty()) {
                                    val copy = HashMap(appBatchBytesMap)
                                    appBatchBytesMap.clear()
                                    copy.forEach { (pkg, bytes) ->
                                        if (bytes > 0L) {
                                            serviceScope.launch {
                                                val updated = usageRepository.recordAppBytes(pkg, bytes)
                                                if (updated != null) {
                                                    managedAppRulesMap[pkg] = updated
                                                    if (updated.isLimitReached) {
                                                        handleAppCutoffNotification(pkg, updated.appDisplayName, isLimitExceeded = true)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                lastBatchFlushTimestamp = now
                            }
                        }
                        is EnforcementDecision.BlockedAppFull -> {
                            handleAppCutoffNotification(decision.packageName, decision.appName, isLimitExceeded = false)
                            val rstPacket = EnforcementRulesEngine.generateCleanTerminationPacket(rawBytes, bytesRead)
                            if (rstPacket != null) {
                                try {
                                    outputStream.write(rstPacket)
                                } catch (_: Exception) {}
                            }
                        }
                        is EnforcementDecision.BlockedAppLimitExceeded -> {
                            handleAppCutoffNotification(decision.packageName, decision.appName, isLimitExceeded = true)
                            val rstPacket = EnforcementRulesEngine.generateCleanTerminationPacket(rawBytes, bytesRead)
                            if (rstPacket != null) {
                                try {
                                    outputStream.write(rstPacket)
                                } catch (_: Exception) {}
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

        // Section 2: SoundPool Cutoff Alert sound
        if (isCutSoundEnabled && cutoffSoundId != 0) {
            try {
                soundPool?.play(cutoffSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
            } catch (_: Exception) {}
        }

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

    private fun handleAppCutoffNotification(
        packageName: String,
        appDisplayName: String,
        isLimitExceeded: Boolean
    ) {
        val key = if (isLimitExceeded) "APP_LIMIT_$packageName" else "APP_BLOCKED_$packageName"
        if (lastCutoffNotificationKey == key) return
        lastCutoffNotificationKey = key

        // Section 5: Haptic feedback and sound for cutoff moment
        hapticController.triggerCutoffHaptic()
        if (isCutSoundEnabled && cutoffSoundId != 0) {
            try {
                soundPool?.play(cutoffSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
            } catch (_: Exception) {}
        }

        if (isLimitExceeded) {
            AppControlNotificationHelper.notifyAppLimitExceeded(applicationContext, appDisplayName, packageName)
            serviceScope.launch {
                usageRepository.logActivity(
                    eventType = ActivityLogEntry.EventType.DISCONNECTED,
                    reason = ActivityLogEntry.SpecificCutReason.APP_LIMIT_EXCEEDED,
                    description = "$appDisplayName وصل لحده اليومي المخصص وتم إيقاف بياناته.",
                    details = packageName
                )
            }
        } else {
            AppControlNotificationHelper.notifyAppFullyBlocked(applicationContext, appDisplayName, packageName)
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

    /**
     * Section 1: Hotspot Adaptive Background Polling Job.
     * Adheres strictly to HotspotAdaptiveEngine intervals (5m default, 1m floor, 99% margin, 10s debounce).
     * Never performs cutoff - alerts only!
     */
    private fun startHotspotMonitoring() {
        hotspotPollingJob?.cancel()
        hotspotPollingJob = serviceScope.launch {
            while (isActive) {
                val intervalMs = hotspotState.nextScheduledIntervalMs.coerceIn(
                    HotspotAdaptiveEngine.MINIMUM_INTERVAL_MS,
                    HotspotAdaptiveEngine.DEFAULT_INTERVAL_MS
                )
                delay(intervalMs)
                try {
                    val now = System.currentTimeMillis()
                    val bytes = networkStatsHelper.queryHotspotBytesToday()
                    val settings = usageRepository.observeSettings().first()
                    val threshold = if (settings.hotspotAlertEnabled) settings.hotspotAlertThresholdBytes else null
                    val wasAlertTriggered = hotspotState.alertTriggeredToday

                    hotspotState = HotspotAdaptiveEngine.processPoll(
                        currentState = hotspotState,
                        newHotspotBytes = bytes,
                        checkTimestamp = now,
                        thresholdBytes = threshold
                    )

                    usageRepository.updateHotspotBytes(bytes)

                    // 10-second debounce delay if scheduled
                    val scheduledAt = hotspotState.pendingNotificationScheduledAt
                    if (scheduledAt != null && !hotspotState.alertTriggeredToday) {
                        val waitTime = scheduledAt - now
                        if (waitTime > 0L) {
                            delay(waitTime)
                        }
                        val recheckNow = System.currentTimeMillis()
                        hotspotState = HotspotAdaptiveEngine.processPoll(
                            currentState = hotspotState,
                            newHotspotBytes = networkStatsHelper.queryHotspotBytesToday(),
                            checkTimestamp = recheckNow,
                            thresholdBytes = threshold
                        )
                    }

                    if (hotspotState.alertTriggeredToday && !wasAlertTriggered && settings.hotspotAlertEnabled) {
                        sendHotspotAlertNotification(bytes, threshold ?: 0L)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun sendHotspotAlertNotification(usedBytes: Long, thresholdBytes: Long) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            3,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, HOTSPOT_ALERT_CHANNEL_ID)
            .setContentTitle("تنبيه استهلاك نقطة الاتصال")
            .setContentText("بلغ استهلاك الهوت سبوت ${SmartUnitFormatter.formatDisplay(usedBytes)} (العتبة: ${SmartUnitFormatter.formatDisplay(thresholdBytes)}). تنبيه فقط دون قطع.")
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager?.notify(HOTSPOT_NOTIFICATION_ID, notification)
    }

    @Synchronized
    private fun stopVpnTunnel() {
        packetLoopJob?.cancel()
        packetLoopJob = null
        periodicCalibrationJob?.cancel()
        periodicCalibrationJob = null
        hotspotPollingJob?.cancel()
        hotspotPollingJob = null

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
        soundPool?.release()
        soundPool = null
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

            // Hotspot alert channel (Section 1)
            val hotspotChannel = NotificationChannel(
                HOTSPOT_ALERT_CHANNEL_ID,
                "تنبيهات نقطة الاتصال",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعار تحذيري عند تجاوز عتبة استهلاك نقطة الاتصال دون قطع البيانات"
            }
            manager?.createNotificationChannel(hotspotChannel)
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
        private const val HOTSPOT_NOTIFICATION_ID = 103
        private const val CHANNEL_ID = "vlast_monitoring_channel"
        private const val CUTOFF_CHANNEL_ID = "vlast_cutoff_channel"
        private const val HOTSPOT_ALERT_CHANNEL_ID = "vlast_hotspot_alert_channel"
    }
}
