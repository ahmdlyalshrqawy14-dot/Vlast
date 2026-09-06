package com.example

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.core.viewmodel.VlastMechanicsViewModel
import com.example.core.vpn.VlastVpnService
import com.example.ui.navigation.VlastAppNavigationShell

/**
 * MainActivity for Vlast:
 * Hosts the application's root Compose navigation shell, handles VPN permission preparation,
 * and boots the local VPN monitoring service safely.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: VlastMechanicsViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVlastVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prepare and ensure VPN service runs
        checkAndStartVpn()

        setContent {
            VlastAppNavigationShell(viewModel = viewModel)
        }
    }

    private fun checkAndStartVpn() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            startVlastVpnService()
        }
    }

    private fun startVlastVpnService() {
        val serviceIntent = Intent(this, VlastVpnService::class.java).apply {
            action = VlastVpnService.ACTION_START_VPN
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
