package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens

/**
 * Item 24: Sovereign "About Vlast" Visual Card Component.
 * Displays the etymology, meaning, and design philosophy of the name Vlast with high-fidelity visual styling.
 */
@Composable
fun AboutVlastVisualCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusLarge))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        VlastTokens.DarkSurface,
                        VlastTokens.DarkSurfaceVariant
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    colors = listOf(
                        VlastTokens.BrandRed,
                        VlastTokens.BrandCyan,
                        VlastTokens.BrandAmber
                    )
                ),
                RoundedCornerShape(VlastTokens.RadiusLarge)
            )
            .padding(VlastTokens.Space20)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
        ) {
            // Crest Header
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(VlastTokens.BrandCyan.copy(alpha = 0.15f))
                    .border(1.5.dp, VlastTokens.BrandCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = VlastTokens.BrandCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Vlast",
                fontFamily = NumberFontFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = VlastTokens.BrandCyan
            )

            // Etymology Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                    .background(VlastTokens.BrandAmber.copy(alpha = 0.15f))
                    .border(1.dp, VlastTokens.BrandAmber.copy(alpha = 0.4f), RoundedCornerShape(VlastTokens.RadiusSmall))
                    .padding(horizontal = VlastTokens.Space12, vertical = VlastTokens.Space4)
            ) {
                Text(
                    text = "معنى الاسم: السلطة السيادية والتحكم المباشر المطلق",
                    color = VlastTokens.BrandAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "تعود كلمة Vlast إلى الجذور التي ترمز إلى القوة الحاكمة والسلطة الفاصلة. تم تصميم هذا النظام ليكون سلطتك الشخصية المطلقة في التحكم ببيانات الهاتف والإنترنت، حيث يتخذ القرار الصارم بالقطع دون تهاون أو استنزاف مالي.",
                color = VlastTokens.TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = VlastTokens.Space4)
            )

            Spacer(modifier = Modifier.height(VlastTokens.Space8))

            // 3 Sovereign Pillars Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
            ) {
                PillarItem(
                    icon = Icons.Filled.VerifiedUser,
                    iconColor = VlastTokens.BrandCyan,
                    title = "سيادة محلية 100%",
                    desc = "يعمل بالكامل داخل جهازك دون إرسال بياناتك أو استهلاكها عبر خوادم خارجية."
                )
                PillarItem(
                    icon = Icons.Filled.Bolt,
                    iconColor = VlastTokens.BrandRed,
                    title = "قطع حاسم وفوري",
                    desc = "إنهاء تنفيذي مباشر لحزم TCP RST لمنع تسرب البيانات أو تجاوز حدود الاستهلاك."
                )
                PillarItem(
                    icon = Icons.Filled.Security,
                    iconColor = VlastTokens.BrandAmber,
                    title = "شجرة أولوية صارمة",
                    desc = "نظام تحكم ثنائي الطبقات يضمن تطبيق القواعد بأعلى كفاءة وعدم التعارض."
                )
            }
        }
    }
}

@Composable
private fun PillarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    desc: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
            .background(VlastTokens.DarkSurfaceVariant)
            .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusSmall))
            .padding(VlastTokens.Space12)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = VlastTokens.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = desc,
                    color = VlastTokens.TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
