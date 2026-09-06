package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.sim.DualSimManager
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens

/**
 * Screen 4: Settings & About Vlast (Items 2, 5, 13, 14, 16, 18, 24, 29):
 * - Dark Mode / Light Mode toggle (Item 2)
 * - Dual SIM selection (Item 13)
 * - Settings PIN lock toggle (Item 16)
 * - Dedicated cut sound toggle (Item 29)
 * - Clarification of Android VPN Key (Item 14)
 * - Rich "About Vlast" displaying authority & control meaning (Item 24 & 18)
 */
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    activeSimSlot: Int,
    availableSims: List<DualSimManager.SimSlotInfo>,
    onSelectSim: (Int) -> Unit,
    isPinLockEnabled: Boolean,
    onTogglePinLock: (Boolean) -> Unit,
    isSoundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VlastTokens.DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VlastTokens.Space16, vertical = VlastTokens.Space8),
        verticalArrangement = Arrangement.spacedBy(VlastTokens.Space16)
    ) {
        Text(
            text = "الإعدادات العامة",
            color = VlastTokens.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = VlastTokens.Space8)
        )

        // Appearance: Dark / Light Mode (Item 2)
        SettingCard(
            icon = Icons.Filled.DarkMode,
            title = "المظهر الداكن (Dark Mode)",
            subtitle = "الوضع الليلي الافتراضي ذو الطابع القوي والتباين العالي"
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

        // Dual SIM Controls (Item 13)
        if (availableSims.size > 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                    .background(VlastTokens.DarkSurface)
                    .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusMedium))
                    .padding(VlastTokens.Space16)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(VlastTokens.Space8)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
                    ) {
                        Icon(Icons.Filled.SimCard, contentDescription = null, tint = VlastTokens.BrandCyan)
                        Text(
                            text = "الشريحة النشطة لإدارة البيانات (Dual SIM)",
                            color = VlastTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
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
        }

        // PIN Security Lock for Settings (Item 16)
        SettingCard(
            icon = Icons.Filled.Lock,
            title = "قفل شاشة الإعدادات برمز PIN",
            subtitle = "حماية إضافية تمنع التلاعب بالحدود أو تخفيضها بدون مصادقة"
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

        // Dedicated Cut Sound System (Item 29)
        SettingCard(
            icon = Icons.Filled.VolumeUp,
            title = "صوت التنبيه الخاص عند القطع",
            subtitle = "نغمة حصرية قصيرة جدًا (0.5 ثانية) عند لحظة قطع الاتصال"
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
                        text = "لماذا تظهر أيقونة المفتاح في شريط الحالة؟",
                        color = VlastTokens.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "نظام أندرويد يفرض ظهور أيقونة المفتاح إجباريًا لأي تطبيق يستخدم واجهة VpnService المحلية. تطبيق Vlast لا يُرسل أي بيانات إلى خوادم خارجية؛ هو نفق محلي تمامًا داخل جهازك فقط لقياس البايتات وفرض الحدود بدون روت.",
                    color = VlastTokens.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // About Vlast (Items 18 & 24)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VlastTokens.RadiusLarge))
                .background(VlastTokens.DarkSurface)
                .border(1.dp, VlastTokens.BrandRedDark, RoundedCornerShape(VlastTokens.RadiusLarge))
                .padding(VlastTokens.Space20)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
            ) {
                Text(
                    text = "Vlast",
                    fontFamily = NumberFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = VlastTokens.BrandRed
                )
                Text(
                    text = "السلطة والتحكم والسيطرة الكاملة",
                    color = VlastTokens.BrandAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "كلمة Vlast تعني السلطة والحكم والسيطرة الحاسمة. صُمم هذا النظام ليمنحك السيادة المطلقة على استهلاك حزم بيانات الإنترنت الخاصة بك، وضمان عدم استنزافها أبدًا خارج الحدود التي ترسمها بنفسك.",
                    color = VlastTokens.TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = VlastTokens.Space8)
                )
            }
        }
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
