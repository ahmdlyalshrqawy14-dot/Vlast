package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Loop
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens

/**
 * Service Cards Architecture (Items 9 & 12):
 *
 * Each of the three mechanisms is built as an independent, clearly bordered Card
 * with its own distinct, unique icon (Item 9):
 * 1. Recurring Daily Limit: Loop icon (تكرار مستمر)
 * 2. Today's Temporary Limit: Calendar icon (اليوم فقط)
 * 3. Manual Kill Switch: Direct Block shield icon (القطع الكامل)
 */

@Composable
fun ServiceCardContainer(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    isOverridden: Boolean = false,
    overrideNotice: String? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
            .background(if (isActive) VlastTokens.DarkSurface else VlastTokens.DarkSurfaceVariant)
            .border(
                1.dp,
                if (isActive) iconColor.copy(alpha = 0.5f) else VlastTokens.DarkBorder,
                RoundedCornerShape(VlastTokens.RadiusMedium)
            )
            .padding(VlastTokens.Space16)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
        ) {
            // Header: Icon + Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                            .background(iconColor.copy(alpha = 0.15f))
                            .border(1.dp, iconColor.copy(alpha = 0.3f), RoundedCornerShape(VlastTokens.RadiusSmall)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = VlastTokens.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = VlastTokens.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Active / Inactive Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VlastTokens.RadiusPill))
                        .background(if (isActive) iconColor.copy(alpha = 0.2f) else VlastTokens.DarkBackground)
                        .border(
                            1.dp,
                            if (isActive) iconColor else VlastTokens.DarkBorder,
                            RoundedCornerShape(VlastTokens.RadiusPill)
                        )
                        .padding(horizontal = VlastTokens.Space8, vertical = VlastTokens.Space2)
                ) {
                    Text(
                        text = if (isActive) "نشط" else "معطّل",
                        color = if (isActive) iconColor else VlastTokens.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Conflict / Priority notice (if another higher mechanism overrides this)
            if (isOverridden && overrideNotice != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                        .background(VlastTokens.BrandAmber.copy(alpha = 0.1f))
                        .border(1.dp, VlastTokens.BrandAmber.copy(alpha = 0.3f), RoundedCornerShape(VlastTokens.RadiusSmall))
                        .padding(VlastTokens.Space8)
                ) {
                    Text(
                        text = overrideNotice,
                        color = VlastTokens.BrandAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            content()
        }
    }
}

/**
 * Service 1: Recurring Daily Limit Card
 * Distinct Icon: Loop (تكرار مستمر)
 */
@Composable
fun RecurringLimitCard(
    currentLimitFormatted: String,
    isEnabled: Boolean,
    onEditLimit: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    isOverriddenByToday: Boolean,
    modifier: Modifier = Modifier
) {
    ServiceCardContainer(
        title = "الحد الدائم اليومي",
        subtitle = "يتجدد تلقائيًا كل يوم عند منتصف الليل",
        icon = Icons.Filled.Loop,
        iconColor = VlastTokens.BrandCyan,
        isActive = isEnabled,
        isOverridden = isOverriddenByToday,
        overrideNotice = "ملاحظة: هذا الحد خاضع حاليًا لتجاوز مؤقت عبر 'حد اليوم'.",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "القيمة المحددة:", color = VlastTokens.TextMuted, fontSize = 11.sp)
                Text(
                    text = if (isEnabled) currentLimitFormatted else "معطل (استهلاك حر)",
                    fontFamily = NumberFontFamily,
                    color = VlastTokens.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
            ) {
                OutlinedButton(
                    onClick = onEditLimit,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VlastTokens.BrandCyan)
                ) {
                    Text("تعديل", fontSize = 12.sp)
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VlastTokens.BrandCyan,
                        checkedTrackColor = VlastTokens.BrandCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = VlastTokens.TextMuted,
                        uncheckedTrackColor = VlastTokens.DarkBackground
                    )
                )
            }
        }
    }
}

/**
 * Service 2: Today's Temporary Limit Card
 * Distinct Icon: CalendarToday (اليوم فقط)
 */
@Composable
fun TodayOverrideCard(
    overrideFormatted: String?,
    isActive: Boolean,
    onSetTodayLimit: () -> Unit,
    onClearTodayLimit: () -> Unit,
    modifier: Modifier = Modifier
) {
    ServiceCardContainer(
        title = "حد اليوم المؤقت (استثناء)",
        subtitle = "يسري لليوم فقط ويُلغى تلقائيًا عند منتصف الليل",
        icon = Icons.Filled.CalendarToday,
        iconColor = VlastTokens.BrandAmber,
        isActive = isActive,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "الحد الاستثنائي لليوم:", color = VlastTokens.TextMuted, fontSize = 11.sp)
                Text(
                    text = if (isActive && overrideFormatted != null) overrideFormatted else "غير مفعل (يعتمد الحد الدائم)",
                    fontFamily = NumberFontFamily,
                    color = if (isActive) VlastTokens.BrandAmber else VlastTokens.TextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
            ) {
                if (isActive) {
                    val haptic = LocalHapticFeedback.current
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                            .background(VlastTokens.SemanticRed.copy(alpha = 0.12f))
                            .border(1.dp, VlastTokens.SemanticRed.copy(alpha = 0.5f), RoundedCornerShape(VlastTokens.RadiusSmall))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onClearTodayLimit()
                                    }
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "إلغاء", tint = VlastTokens.SemanticRed, modifier = Modifier.size(14.dp))
                            Text("إلغاء (ضغط مطول)", color = VlastTokens.SemanticRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = onSetTodayLimit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VlastTokens.BrandAmber,
                        contentColor = VlastTokens.DarkBackground
                    )
                ) {
                    Text(if (isActive) "تغيير" else "تحديد لليوم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Service 3: Manual Kill Switch Card
 * Distinct Icon: Block (القطع الكامل الفوري)
 */
@Composable
fun KillSwitchCard(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showLongPressHint by remember { mutableStateOf(false) }

    ServiceCardContainer(
        title = "القطع الكامل اليدوي",
        subtitle = "حظر فوري وشامل لجميع البيانات (أعلى أولوية لا تلغى تلقائيًا)",
        icon = Icons.Filled.Block,
        iconColor = VlastTokens.BrandRed,
        isActive = isActive,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
        ) {
            Text(
                text = if (isActive)
                    "⚠️ القطع الكامل نشط: جميع حركات الإنترنت على Wi-Fi وبيانات الهاتف محظورة تمامًا."
                else
                    "لحماية اتصالك، يتطلب تفعيل القطع الكامل ضغطة مطوّلة منعاً للمس بالخطأ.",
                color = if (isActive) VlastTokens.SemanticRed else VlastTokens.TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (!isActive && showLongPressHint) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                        .background(VlastTokens.BrandRed.copy(alpha = 0.15f))
                        .border(1.dp, VlastTokens.BrandRed.copy(alpha = 0.4f), RoundedCornerShape(VlastTokens.RadiusSmall))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚠️ يتطلب تفعيل القطع الكامل ضغطة مطوّلة مستمرة لمنع التفعيل غير المقصود.",
                        color = VlastTokens.SemanticRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isActive) {
                // Deactivation / Restoring does not require long press or confirmation (Item 7)
                Button(
                    onClick = { onToggle(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VlastTokens.SemanticGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "استئناف الاتصال والإنترنت الآن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // Item 12: Activating Kill Switch requires a long press to prevent accidental touch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                        .background(VlastTokens.BrandRed)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showLongPressHint = false
                                    onToggle(true)
                                },
                                onTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showLongPressHint = true
                                }
                            )
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تفعيل القطع الكامل (اضغط مطوّلاً)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
