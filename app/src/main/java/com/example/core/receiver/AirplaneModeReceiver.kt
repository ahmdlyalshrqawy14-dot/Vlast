package com.example.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.core.database.VlastDatabase
import com.example.core.model.ActivityLogEntry
import com.example.core.repository.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Airplane Mode Receiver (Item 17):
 * Accurately detects Airplane Mode activation/deactivation and records it
 * independently in the Activity Log so it is NEVER misattributed to app limits.
 */
class AirplaneModeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            val isAirplaneModeOn = intent.getBooleanExtra("state", false) ||
                    Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = VlastDatabase.getInstance(context)
                    val repository = UsageRepository(
                        dailyUsageDao = db.dailyUsageDao(),
                        appSettingsDao = db.appSettingsDao(),
                        activityLogDao = db.activityLogDao()
                    )

                    if (isAirplaneModeOn) {
                        repository.logActivity(
                            eventType = ActivityLogEntry.EventType.SYSTEM_EVENT,
                            reason = ActivityLogEntry.SpecificCutReason.AIRPLANE_MODE,
                            description = "تم تفعيل وضع الطيران في نظام أندرويد (انقطاع نظامي مستقل عن حدود Vlast)."
                        )
                    } else {
                        repository.logActivity(
                            eventType = ActivityLogEntry.EventType.SYSTEM_EVENT,
                            reason = ActivityLogEntry.SpecificCutReason.CONNECTION_RESTORED,
                            description = "تم إيقاف وضع الطيران واستئناف الاتصال الطبيعي."
                        )
                    }
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
