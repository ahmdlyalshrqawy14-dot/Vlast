package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.sim.DualSimManager
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storefront
import com.example.ui.components.AboutVlastVisualCard
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens

/**
 * Screen 4: Settings & About Vlast (Items 2, 5, 13, 14, 16, 18, 24, 29 & Sections 4, 8, 11):
 * - Dark Mode / Light Mode toggle (Item 2)
 * - Dual SIM selection & READ_PHONE_STATE permission flow (Section 11)
 * - Settings PIN lock toggle (Item 16)
 * - Dedicated cut sound toggle (Section 2)
 * - Color-blind mode toggle (Section 8)
 * - Language selector (Arabic / English) (Section 4)
 * - Clarification of Android VPN Key (Item 14)
 * - Sovereign "About Vlast" (Items 18 & 24)
 * - Per-App Control entrance on Android 10+ (Phase 4)
 */
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    activeSimSlot: Int,
    availableSims: List<DualSimManager.SimSlotInfo>,
    onSelectSim: (Int) -> Unit,
    onRefreshSims: () -> Unit = {},
    isPinLockEnabled: Boolean,
    onTogglePinLock: (Boolean) -> Unit,
    isSoundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    isHapticEnabled: Boolean = true,
    onToggleHaptic: (Boolean) -> Unit = {},
    isColorBlindMode: Boolean,
    onToggleColorBlindMode: (Boolean) -> Unit,
    currentLanguage: String = "ar",
    onSelectLanguage: (String) -> Unit = {},
    onNavigateToAppControl: () -> Unit = {},
    onNavigateToStorePreview: () -> Unit = {},
    onRequestStopProtection: () -> Unit = {},
    onRequestShutdownApp: () -> Unit = {},
    onRequestFactoryResetApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPhonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPhonePermission = granted
        if (granted) {
            onRefreshSims()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VlastTokens.DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VlastTokens.Space16, vertical = VlastTokens.Space8),
        verticalArrangement = Arrangement.spacedBy(VlastTokens.Space16)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            color = VlastTokens.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = VlastTokens.Space8)
        )

        // Appearance: Dark / Light Mode (Item 2)
        SettingCard(
            icon = Icons.Filled.DarkMode,
            title = stringResource(R.string.appearance_dark_mode),
            subtitle = stringResource(R.string.appearance_dark_desc)
        ) {
            Switch(
                checked = isDarkMode,
                onCheckedChange = onToggleDarkMode,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VlastTokens.BrandRed,
                    checkedTrackColor = VlastTokens.BrandRed.copy(alpha = 0.3f),
                    uncheckedThumbColor = VlastTokens.TextMuted,
                    uncheckedTrackColor = VlastTokens.DarkBackground
                )
            )
        }

        // Color-blind Accessible Mode (Section 8)
        SettingCard(
            icon = Icons.Filled.Palette,
            title = stringResource(R.string.color_blind_title),
            subtitle = stringResource(R.string.color_blind_desc)
        ) {
            Switch(
                checked = isColorBlindMode,
                onCheckedChange = onToggleColorBlindMode,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VlastTokens.BrandCyan,
                    checkedTrackColor = VlastTokens.BrandCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = VlastTokens.TextMuted,
                    uncheckedTrackColor = VlastTokens.DarkBackground
                )
            )
        }

        // App Language (Section 4)
        SettingCard(
            icon = Icons.Filled.Language,
            title = stringResource(R.string.language_title),
            subtitle = stringResource(R.string.language_desc)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = { onSelectLanguage("ar") },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (currentLanguage == "ar") VlastTokens.BrandCyan.copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = if (currentLanguage == "ar") VlastTokens.BrandCyan else VlastTokens.TextMuted
                    ),
                    modifier = Modifier.size(width = 68.dp, height = 36.dp)
                ) {
                    Text("عربي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onSelectLanguage("en") },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (currentLanguage == "en") VlastTokens.BrandCyan.copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = if (currentLanguage == "en") VlastTokens.BrandCyan else VlastTokens.TextMuted
                    ),
                    modifier = Modifier.size(width = 68.dp, height = 36.dp)
                ) {
                    Text("EN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Dedicated Cut Sound System (Section 2)
        SettingCard(
            icon = Icons.Filled.VolumeUp,
            title = stringResource(R.string.sound_alert_title),
            subtitle = stringResource(R.string.sound_alert_desc)
        ) {
            Switch(
                checked = isSoundEnabled,
                onCheckedChange = onToggleSound,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VlastTokens.BrandCyan,
                    checkedTrackColor = VlastTokens.BrandCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = VlastTokens.TextMuted,
                    uncheckedTrackColor = VlastTokens.DarkBackground
                )
            )
        }

        // Dedicated Haptic Feedback / Vibration System (Section 15)
        SettingCard(
            icon = Icons.Filled.Vibration,
            title = "تنبيهات الاهتزاز (Haptic)",
            subtitle = "اهتزاز تفاعلي عند الوصول لـ 90% أو عند لحظة قطع الاتصال"
        ) {
            Switch(
                checked = isHapticEnabled,
                onCheckedChange = onToggleHaptic,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VlastTokens.BrandCyan,
                    checkedTrackColor = VlastTokens.BrandCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = VlastTokens.TextMuted,
                    uncheckedTrackColor = VlastTokens.DarkBackground
                )
            )
        }

        // PIN Security Lock for Settings (Item 16 & Section 7)
        SettingCard(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.pin_lock_title),
            subtitle = stringResource(R.string.pin_lock_desc)
        ) {
            Switch(
                checked = isPinLockEnabled,
                onCheckedChange = onTogglePinLock,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VlastTokens.BrandAmber,
                    checkedTrackColor = VlastTokens.BrandAmber.copy(alpha = 0.3f),
                    uncheckedThumbColor = VlastTokens.TextMuted,
                    uncheckedTrackColor = VlastTokens.DarkBackground
                )
            )
        }

        // Dual SIM Controls & Permission (Section 11)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                .background(VlastTokens.DarkSurface)
                .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusMedium))
                .padding(VlastTokens.Space16)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(VlastTokens.Space12)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
                ) {
                    Icon(Icons.Filled.SimCard, contentDescription = null, tint = VlastTokens.BrandCyan)
                    Text(
                        text = stringResource(R.string.dual_sim_title),
                        color = VlastTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!hasPhonePermission) {
                    // Explanatory card for requesting READ_PHONE_STATE permission
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                            .background(VlastTokens.DarkSurfaceVariant)
                            .border(1.dp, VlastTokens.BrandAmber.copy(alpha = 0.4f), RoundedCornerShape(VlastTokens.RadiusSmall))
                            .padding(VlastTokens.Space12)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.read_phone_permission_desc),
                                color = VlastTokens.TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                                colors = ButtonDefaults.buttonColors(containerColor = VlastTokens.BrandAmber, contentColor = VlastTokens.DarkBackground),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(R.string.grant_permission_btn), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSims.forEach { sim ->
                        val isSelected = activeSimSlot == sim.slotIndex
                        Button(
                            onClick = { onSelectSim(sim.slotIndex) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) VlastTokens.BrandCyan else VlastTokens.DarkSurfaceVariant,
                                contentColor = if (isSelected) VlastTokens.DarkBackground else VlastTokens.TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = sim.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Clarification: Android VPN Key Notification (Item 14)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                .background(VlastTokens.DarkSurfaceVariant)
                .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusMedium))
                .padding(VlastTokens.Space16)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(VlastTokens.Space8)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = VlastTokens.BrandCyan)
                    Text(
                        text = stringResource(R.string.vpn_key_info_title),
                        color = VlastTokens.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(R.string.vpn_key_info_desc),
                    color = VlastTokens.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Phase 4: Per-App Control entrance (Strictly visible only on Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                    .background(VlastTokens.DarkSurface)
                    .border(1.dp, VlastTokens.BrandCyan.copy(alpha = 0.5f), RoundedCornerShape(VlastTokens.RadiusMedium))
                    .clickable(onClick = onNavigateToAppControl)
                    .padding(VlastTokens.Space16)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VlastTokens.BrandCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = VlastTokens.BrandCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "التحكم بالتطبيقات",
                                color = VlastTokens.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "حظر اتصال أو تحديد حد استهلاك يومي لكل تطبيق بشكل مستقل",
                                color = VlastTokens.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = VlastTokens.BrandCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Item 32: Google Play Store Preview Showcase Entrance
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                .background(VlastTokens.DarkSurface)
                .border(1.dp, VlastTokens.BrandAmber.copy(alpha = 0.5f), RoundedCornerShape(VlastTokens.RadiusMedium))
                .clickable(onClick = onNavigateToStorePreview)
                .padding(VlastTokens.Space16)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(VlastTokens.BrandAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = VlastTokens.BrandAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "معاينة تصاميم متجر Google Play",
                            color = VlastTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "استعراض البانر التعريفي ولقطات الشاشة المصممة بهوية Vlast الرسمية",
                            color = VlastTokens.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = VlastTokens.BrandAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Section 4: Master Control Buttons (أزرار السيطرة الكاملة)
        Text(
            text = "أزرار السيطرة الكاملة",
            color = VlastTokens.BrandRed,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = VlastTokens.Space8)
        )

        // 4a) Stop Protection / Stop VPN button
        SettingCard(
            icon = Icons.Filled.Security,
            title = "إيقاف الحماية / إيقاف الـ VPN",
            subtitle = "تجميد نفق الـ VPN والفلترة مؤقتاً، والعودة للإنترنت الطبيعي دون أي تدخل من Vlast"
        ) {
            Button(
                onClick = onRequestStopProtection,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VlastTokens.BrandAmber,
                    contentColor = VlastTokens.DarkBackground
                )
            ) {
                Text("إيقاف الـ VPN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 4b) Shutdown Vlast completely button
        SettingCard(
            icon = Icons.Filled.PowerSettingsNew,
            title = "إيقاف التطبيق بالكامل",
            subtitle = "إيقاف كل وظائف Vlast نهائياً (نفق + مراقبة + إشعارات + خدمات خلفية)"
        ) {
            Button(
                onClick = onRequestShutdownApp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VlastTokens.BrandRed.copy(alpha = 0.85f),
                    contentColor = Color.White
                )
            ) {
                Text("إيقاف التشغيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 4c) Full Factory Reset button
        SettingCard(
            icon = Icons.Filled.RestartAlt,
            title = "إعادة تعيين شاملة",
            subtitle = "إعادة التطبيق لحالته الأولى كأنه ثُبّت للتو (حذف كل الحدود والسجلات والإعدادات)"
        ) {
            Button(
                onClick = onRequestFactoryResetApp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VlastTokens.BrandRed,
                    contentColor = Color.White
                )
            ) {
                Text("إعادة تعيين شاملة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Sovereign "About Vlast" (Items 18 & 24)
        AboutVlastVisualCard()
    }
}

@Composable
private fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    action: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
            .background(VlastTokens.DarkSurface)
            .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusMedium))
            .padding(VlastTokens.Space16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = VlastTokens.TextSecondary)
                Column {
                    Text(
                        text = title,
                        color = VlastTokens.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = VlastTokens.TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            action()
        }
    }
}
