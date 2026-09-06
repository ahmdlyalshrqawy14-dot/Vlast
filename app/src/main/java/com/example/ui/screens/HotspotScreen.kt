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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiTethering
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.SmartUnitFormatter
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens

/**
 * Screen 3: Hotspot Monitor & Alert (Phase 1 & Phase 3):
 * Strictly measures hotspot consumption without cutting connection,
 * providing alert threshold controls and manual refresh.
 */
@Composable
fun HotspotScreen(
    hotspotUsedBytes: Long,
    alertThresholdBytes: Long?,
    isAlertEnabled: Boolean,
    onConfigureThreshold: () -> Unit,
    onToggleAlert: (Boolean) -> Unit,
    onManualRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hotspotFormatted = SmartUnitFormatter.format(hotspotUsedBytes)
    val thresholdFormatted = if (alertThresholdBytes != null) SmartUnitFormatter.formatDisplay(alertThresholdBytes) else "غير محدد"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VlastTokens.DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VlastTokens.Space16, vertical = VlastTokens.Space8),
        verticalArrangement = Arrangement.spacedBy(VlastTokens.Space16)
    ) {
        Text(
            text = "نقطة الاتصال (Hotspot)",
            color = VlastTokens.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = VlastTokens.Space8)
        )

        // Notice: Measurement without cutting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                .background(VlastTokens.BrandAmber.copy(alpha = 0.1f))
                .border(1.dp, VlastTokens.BrandAmber.copy(alpha = 0.3f), RoundedCornerShape(VlastTokens.RadiusMedium))
                .padding(VlastTokens.Space12)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = VlastTokens.BrandAmber, modifier = Modifier.size(20.dp))
                Text(
                    text = "وفقًا لمتطلبات Vlast: نقطة الاتصال تقاس بدقة وبشكل مستقل دون تطبيق أي قطع لحركة الأجهزة المتصلة بها.",
                    color = VlastTokens.BrandAmber,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Hero Hotspot Consumption Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VlastTokens.RadiusLarge))
                .background(VlastTokens.DarkSurface)
                .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusLarge))
                .padding(VlastTokens.Space20)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                        .background(VlastTokens.BrandAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiTethering,
                        contentDescription = null,
                        tint = VlastTokens.BrandAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "استهلاك نقطة الاتصال اليوم",
                    color = VlastTokens.TextMuted,
                    fontSize = 12.sp
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = hotspotFormatted.amount,
                        fontFamily = NumberFontFamily,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = VlastTokens.TextPrimary
                    )
                    if (hotspotFormatted.unit.isNotEmpty()) {
                        Text(
                            text = " ${hotspotFormatted.unit}",
                            fontFamily = NumberFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = VlastTokens.BrandAmber,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onManualRefresh,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VlastTokens.BrandAmber),
                    modifier = Modifier.padding(top = VlastTokens.Space8)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "تحديث",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " تحديث قياس الهوت سبوت الآن",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Alert Threshold Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                .background(VlastTokens.DarkSurface)
                .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusMedium))
                .padding(VlastTokens.Space16)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = VlastTokens.BrandCyan)
                        Column {
                            Text(
                                text = "تنبيه تجاوز حد الهوت سبوت",
                                color = VlastTokens.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "إشعار تحذيري فقط عند بلوغ قيمة محددة",
                                color = VlastTokens.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isAlertEnabled,
                        onCheckedChange = onToggleAlert,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VlastTokens.BrandCyan,
                            checkedTrackColor = VlastTokens.BrandCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = VlastTokens.TextMuted,
                            uncheckedTrackColor = VlastTokens.DarkBackground
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عتبة التنبيه: $thresholdFormatted",
                        fontFamily = NumberFontFamily,
                        color = VlastTokens.TextSecondary,
                        fontSize = 13.sp
                    )

                    OutlinedButton(
                        onClick = onConfigureThreshold,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VlastTokens.BrandCyan)
                    ) {
                        Text("تعديل العتبة", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
