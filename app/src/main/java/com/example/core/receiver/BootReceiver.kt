package com.example.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.core.database.VlastDatabase
import com.example.core.vpn.VlastVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recovers Vlast state upon device boot or package update (Section 8).
 * Ensures that if monitoring was previously active, the local tunnel is restored
 * with all recurring limits, overrides, and kill switch settings intact.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = VlastDatabase.getInstance(context)
                    val settings = db.appSettingsDao().getSettingsSync()
                    if (settings?.isMonitoringActive == true) {
                        val serviceIntent = Intent(context, VlastVpnService::class.java).apply {
                            this.action = VlastVpnService.ACTION_START_VPN
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
