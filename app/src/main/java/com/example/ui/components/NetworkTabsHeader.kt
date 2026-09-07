package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.NetworkType
import com.example.core.viewmodel.DashboardNetworkTab
import com.example.ui.theme.VlastTokens

/**
 * 3-Section Functional Network Tab Header (Wi-Fi, SIM 1, SIM 2).
 * Allows switching between control interfaces freely at any time.
 */
@Composable
fun NetworkTabsHeader(
    selectedTab: DashboardNetworkTab,
    activeNetwork: NetworkType,
    activeSimSlot: Int,
    onSelectTab: (DashboardNetworkTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
            .background(VlastTokens.DarkSurface)
            .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusMedium))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 1: Wi-Fi
            val isWifiActiveNow = activeNetwork == NetworkType.WIFI
            TabItem(
                title = "الواي فاي",
                isSelected = selectedTab == DashboardNetworkTab.WIFI,
                isActiveNow = isWifiActiveNow,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = { onSelectTab(DashboardNetworkTab.WIFI) },
                modifier = Modifier.weight(1f)
            )

            // Tab 2: SIM 1
            val isSim1ActiveNow = activeNetwork == NetworkType.MOBILE && activeSimSlot == 0
            TabItem(
                title = "الشريحة 1",
                isSelected = selectedTab == DashboardNetworkTab.SIM_1,
                isActiveNow = isSim1ActiveNow,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.SimCard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = { onSelectTab(DashboardNetworkTab.SIM_1) },
                modifier = Modifier.weight(1f)
            )

            // Tab 3: SIM 2
            val isSim2ActiveNow = activeNetwork == NetworkType.MOBILE && activeSimSlot == 1
            TabItem(
                title = "الشريحة 2",
                isSelected = selectedTab == DashboardNetworkTab.SIM_2,
                isActiveNow = isSim2ActiveNow,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.SimCard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = { onSelectTab(DashboardNetworkTab.SIM_2) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabItem(
    title: String,
    isSelected: Boolean,
    isActiveNow: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) VlastTokens.BrandCyan.copy(alpha = 0.18f) else Color.Transparent,
        label = "tab_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) VlastTokens.BrandCyan else Color.Transparent,
        label = "tab_border"
    )
    val textColor = if (isSelected) VlastTokens.BrandCyan else VlastTokens.TextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(VlastTokens.RadiusSmall))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                icon()
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }

            if (isActiveNow) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(VlastTokens.SemanticGreen)
                    )
                    Text(
                        text = "متصل",
                        fontSize = 9.sp,
                        color = VlastTokens.SemanticGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
