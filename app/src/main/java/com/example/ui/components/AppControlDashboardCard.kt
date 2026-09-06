package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VlastTokens

/**
 * Section 6: Prominent Per-App Control Card on Main Dashboard Screen (Item 4).
 * Allows direct, 1-tap access to per-app firewall and custom daily limits
 * directly from the main screen without entering Settings.
 */
@Composable
fun AppControlDashboardCard(
    activeRulesCount: Int,
    onNavigateToAppControl: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
            .background(VlastTokens.DarkSurface)
            .border(
                width = 1.dp,
                color = if (activeRulesCount > 0) VlastTokens.BrandAmber.copy(alpha = 0.5f) else VlastTokens.DarkBorder,
                shape = RoundedCornerShape(VlastTokens.RadiusMedium)
            )
            .clickable(onClick = onNavigateToAppControl)
            .padding(VlastTokens.Space16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                    .background(VlastTokens.BrandCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = VlastTokens.BrandCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "التحكم بالتطبيقات",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VlastTokens.TextPrimary
                    )
                    if (activeRulesCount > 0) {
                        Spacer(modifier = Modifier.width(VlastTokens.Space8))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(VlastTokens.BrandAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$activeRulesCount قاعدة نشطة",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VlastTokens.BrandAmber
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "حظر أو تحديد الاستهلاك اليومي لكل تطبيق بشكل مستقل",
                    fontSize = 12.sp,
                    color = VlastTokens.TextSecondary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "فتح التحكم بالتطبيقات",
                tint = VlastTokens.BrandCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
