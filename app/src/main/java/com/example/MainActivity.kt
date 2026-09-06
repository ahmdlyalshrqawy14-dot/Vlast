package com.example

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.core.viewmodel.VlastMechanicsViewModel
import com.example.core.vpn.VlastVpnService
import com.example.ui.navigation.VlastAppNavigationShell

/**
 * MainActivity for Vlast:
 * Hosts the application's root Compose navigation shell, handles VPN permission preparation,
 * boots the local VPN monitoring service safely, installs splash screen, and handles dynamic shortcuts.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: VlastMechanicsViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVlastVpnService()
        } else {
            // Item 22: Trigger branded VPN permission denied error dialog
            viewModel.triggerVpnErrorDialog(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Item 20: Official Android 12+ Splash Screen with calculated exit scale & fade animation
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val iconView = splashScreenViewProvider.iconView
            iconView.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .alpha(0f)
                .setDuration(450L)
                .withEndAction {
                    splashScreenViewProvider.remove()
                }
                .start()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Shortcuts (Section 6)
        handleShortcutIntent(intent)

        // Prepare and ensure VPN service runs
        checkAndStartVpn()

        setContent {
            VlastAppNavigationShell(
                viewModel = viewModel,
                onLanguageChange = { langTag ->
                    val appLocale = LocaleListCompat.forLanguageTags(langTag)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        when (intent?.action) {
            "com.example.ACTION_SHORTCUT_KILL_SWITCH" -> {
                val current = viewModel.settings.value.killSwitchActive
                viewModel.requestKillSwitchToggle(!current)
            }
            "com.example.ACTION_SHORTCUT_REMAINING" -> {
                // Navigates directly to dashboard (default screen)
            }
        }
    }

    fun checkAndStartVpn() {
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
