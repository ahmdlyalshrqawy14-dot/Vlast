package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VlastTokens

/**
 * Items 5 & 14: First-Run Onboarding & Explanation Dialog.
 *
 * Provides:
 * - Clear, concise differentiation between the 3 core services:
 *   1. Recurring Limit (الحد الدائم)
 *   2. Today's Temporary Limit (حد اليوم المؤقت)
 *   3. Manual Kill Switch (القطع الكامل)
 * - Clear explanation of the Android VPN Key icon behavior in the status bar:
 *   Mandatory system indicator, purely local on-device filtering, NOT a security breach.
 */
@Composable
fun FirstRunExplanationDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VlastTokens.DarkSurface,
        titleContentColor = VlastTokens.TextPrimary,
        textContentColor = VlastTokens.TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VlastTokens.BrandCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = VlastTokens.BrandCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "مرحباً بك في Vlast",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = VlastTokens.TextPrimary
                    )
                    Text(
                        text = "دليلك السريع للتحكم الصارم في البيانات",
                        fontSize = 11.sp,
                        color = VlastTokens.TextMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
            ) {
                Text(
                    text = "نظام Vlast يمنحك ثلاث خدمات حماية رئيسية، إليك الفرق بينها:",
                    fontSize = 12.sp,
                    color = VlastTokens.TextSecondary,
                    lineHeight = 17.sp
                )

                // Service 1: Recurring Limit
                ServiceExplanationItem(
                    icon = Icons.Filled.Loop,
                    iconColor = VlastTokens.BrandCyan,
                    title = "1. الحد الدائم (Daily Recurring)",
                    description = "سقف استهلاك يتجدد تلقائيًا كل يوم عند منتصف الليل. مصمم لضبط استهلاكك اليومي المعتاد وحمايتك من نفاذ الباقة."
                )

                // Service 2: Today Override
                ServiceExplanationItem(
                    icon = Icons.Filled.CalendarToday,
                    iconColor = VlastTokens.BrandAmber,
                    title = "2. حد اليوم المؤقت (Today Override)",
                    description = "سقف استثنائي يسري لليوم الحالي فقط ويلغى تلقائيًا عند منتصف الليل، دون المساس بحدك الدائم للأيام التالية."
                )

                // Service 3: Kill Switch
                ServiceExplanationItem(
                    icon = Icons.Filled.Block,
                    iconColor = VlastTokens.BrandRed,
                    title = "3. القطع الكامل (Kill Switch)",
                    description = "حظر يدوي فوري وشامل لجميع اتصالات الإنترنت بلا استثناء ولأعلى أولوية في النظام، ولا يُلغى إلا بتدخلك اليدوي."
                )

                // Item 14: Clarification of Android VPN Key
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                        .background(VlastTokens.DarkSurfaceVariant)
                        .border(1.dp, VlastTokens.BrandAmber.copy(alpha = 0.3f), RoundedCornerShape(VlastTokens.RadiusMedium))
                        .padding(VlastTokens.Space12)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VpnKey,
                            contentDescription = null,
                            tint = VlastTokens.BrandAmber,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = "توضيح: أيقونة المفتاح (VPN) في شريط الحالة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VlastTokens.BrandAmber
                            )
                            Text(
                                text = "ظهور أيقونة المفتاح هو سلوك إلزامي يفرضه نظام أندرويد عند تشغيل أي فلترة للشبكة. نفق Vlast يعمل محليًا بالكامل داخل جهازك بنسبة 100% دون أي خوادم خارجية، وظهور المفتاح يؤكد نشاط الحماية المحلية وليس مؤشرًا لاختراق.",
                                fontSize = 11.sp,
                                color = VlastTokens.TextSecondary,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VlastTokens.BrandCyan,
                    contentColor = VlastTokens.DarkBackground
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "فهمت، ابدأ الاستخدام الآن",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    )
}

@Composable
private fun ServiceExplanationItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
            .background(VlastTokens.DarkSurfaceVariant)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VlastTokens.TextPrimary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = VlastTokens.TextSecondary,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
