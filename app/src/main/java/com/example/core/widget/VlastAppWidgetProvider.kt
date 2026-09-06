package com.example.core.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.example.R
import com.example.core.database.VlastDatabase
import com.example.core.model.DailyUsageRecord
import com.example.core.model.NetworkType
import com.example.core.model.SmartUnitFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clean compact Home Screen Widget (Item 8 & Section 9):
 * Supports dynamic resizing:
 * - < 180dp: vlast_widget_small.xml
 * - >= 180dp: vlast_app_widget_layout.xml
 */
class VlastAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, VlastAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            updateWidgets(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VlastDatabase.getInstance(context)
                val today = java.time.LocalDate.now().toString()
                val entity = db.dailyUsageDao().getDailyRecordSync(today)
                val domainRecord: DailyUsageRecord? = entity?.toDomain()
                val settings = db.appSettingsDao().getSettingsSync()

                val wifiRemaining: Long? = domainRecord?.getRemainingBytes(NetworkType.WIFI, 0)
                val mobileRemaining: Long? = domainRecord?.getRemainingBytes(NetworkType.MOBILE, settings?.activeSimSlot ?: 0)
                val isKillSwitch = settings?.killSwitchActive == true || domainRecord?.killSwitchEnabled == true

                val wifiText = if (wifiRemaining != null) SmartUnitFormatter.formatDisplay(wifiRemaining) else "∞"
                val mobileText = if (mobileRemaining != null) SmartUnitFormatter.formatDisplay(mobileRemaining) else "∞"
                val killSwitchText = if (isKillSwitch) "مفعل (نشط)" else "متوقف"

                for (widgetId in appWidgetIds) {
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 200

                    val views = if (minWidth < 180) {
                        RemoteViews(context.packageName, R.layout.vlast_widget_small).apply {
                            val primaryRemaining = if (mobileRemaining != null) mobileText else wifiText
                            setTextViewText(R.id.widget_small_remaining_value, primaryRemaining)
                            val statusText = if (isKillSwitch) "مفعل (قطع)" else if (mobileRemaining != null) "موبايل متبقي" else "واي فاي متبقي"
                            setTextViewText(R.id.widget_small_status_text, statusText)
                        }
                    } else {
                        RemoteViews(context.packageName, R.layout.vlast_app_widget_layout).apply {
                            setTextViewText(R.id.widget_wifi_remaining_text, wifiText)
                            setTextViewText(R.id.widget_mobile_remaining_text, mobileText)
                            setTextViewText(R.id.widget_kill_switch_status_text, killSwitchText)
                        }
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.core.widget.UPDATE_WIDGET"

        fun notifyUpdate(context: Context) {
            val intent = Intent(context, VlastAppWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
