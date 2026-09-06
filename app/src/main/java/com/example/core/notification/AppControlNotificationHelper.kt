package com.example.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * Section 5: Dedicated notification dispatcher for Per-App control events.
 * Guarantees explicit notification for:
 * 1. Immediate full block ("تم حظر [اسم التطبيق] بالكامل من الإنترنت").
 * 2. Dedicated daily limit breach ("[اسم التطبيق] وصل لحده اليومي المخصص وتم إيقاف بياناته").
 * 3. Unblocking or limit expansion ("تم إلغاء حظر [اسم التطبيق] واستعادة اتصاله بالإنترنت").
 */
object AppControlNotificationHelper {

    private const val APP_CUTOFF_CHANNEL_ID = "vlast_cutoff_channel"
    private const val APP_INFO_CHANNEL_ID = "vlast_app_info_channel"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            val infoChannel = NotificationChannel(
                APP_INFO_CHANNEL_ID,
                "إشعارات إدارة التطبيقات",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات تفعيل أو إلغاء حظر التطبيقات وتغيير حدودها"
            }
            manager.createNotificationChannel(infoChannel)
        }
    }

    fun notifyAppFullyBlocked(context: Context, appDisplayName: String, packageName: String) {
        ensureChannels(context)
        val title = "حظر اتصال التطبيق"
        val message = "تم حظر $appDisplayName بالكامل من الإنترنت."
        sendNotification(context, APP_INFO_CHANNEL_ID, packageName.hashCode() + 1000, title, message, NotificationCompat.PRIORITY_HIGH)
    }

    fun notifyAppUnblocked(context: Context, appDisplayName: String, packageName: String) {
        ensureChannels(context)
        val title = "استعادة اتصال التطبيق"
        val message = "تم إلغاء حظر $appDisplayName واستعادة اتصاله بالإنترنت."
        sendNotification(context, APP_INFO_CHANNEL_ID, packageName.hashCode() + 2000, title, message, NotificationCompat.PRIORITY_DEFAULT)
    }

    fun notifyAppLimitExceeded(context: Context, appDisplayName: String, packageName: String) {
        ensureChannels(context)
        val title = "تجاوز الحد المخصص للتطبيق"
        val message = "$appDisplayName وصل لحده اليومي المخصص وتم إيقاف بياناته."
        sendNotification(context, APP_CUTOFF_CHANNEL_ID, packageName.hashCode() + 3000, title, message, NotificationCompat.PRIORITY_HIGH)
    }

    fun notifyAppLimitUpdated(context: Context, appDisplayName: String, packageName: String, limitStr: String) {
        ensureChannels(context)
        val title = "تحديث حد التطبيق"
        val message = "تم تفعيل حد يومي مخصص ($limitStr) لتطبيق $appDisplayName."
        sendNotification(context, APP_INFO_CHANNEL_ID, packageName.hashCode() + 4000, title, message, NotificationCompat.PRIORITY_DEFAULT)
    }

    private fun sendNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        priority: Int
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: Exception) {}
    }
}
