package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplaneTicket
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ActivityLogEntry
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen 2: Activity Logs & Historical Reports (Items 10, 11, 13, 17):
 * Displays detailed audit entries with specific reasons, timestamps, and filters.
 */
@Composable
fun ReportsScreen(
    logs: List<ActivityLogEntry>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<ActivityLogEntry.EventType?>(null) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss  yyyy-MM-dd", Locale.US) }

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == null) logs else logs.filter { it.eventType == selectedFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VlastTokens.DarkBackground)
            .padding(horizontal = VlastTokens.Space16, vertical = VlastTokens.Space8)
    ) {
        // Screen Header
        Text(
            text = "سجل الأنشطة وانقطاع البيانات",
            color = VlastTokens.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = VlastTokens.Space8, bottom = VlastTokens.Space4)
        )
        Text(
            text = "توثيق رسمي دقيق لكل لحظة قطع أو استئناف مع بيان السبب الصريح",
            color = VlastTokens.TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = VlastTokens.Space12)
        )

        // Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = VlastTokens.Space12),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPill(
                label = "الكل (${logs.size})",
                isSelected = selectedFilter == null,
                onClick = { selectedFilter = null }
            )
            FilterPill(
                label = "انقطاع",
                isSelected = selectedFilter == ActivityLogEntry.EventType.DISCONNECTED,
                onClick = { selectedFilter = ActivityLogEntry.EventType.DISCONNECTED }
            )
            FilterPill(
                label = "استئناف",
                isSelected = selectedFilter == ActivityLogEntry.EventType.RESTORED,
                onClick = { selectedFilter = ActivityLogEntry.EventType.RESTORED }
            )
            FilterPill(
                label = "نظامي",
                isSelected = selectedFilter == ActivityLogEntry.EventType.SYSTEM_EVENT,
                onClick = { selectedFilter = ActivityLogEntry.EventType.SYSTEM_EVENT }
            )
        }

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد سجلات مطابقة في الوقت الحالي.",
                    color = VlastTokens.TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs) { entry ->
                    LogItemCard(entry = entry, formattedTime = timeFormatter.format(Date(entry.timestamp)))
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
            .background(if (isSelected) VlastTokens.BrandCyan else VlastTokens.DarkSurfaceVariant)
            .border(1.dp, if (isSelected) VlastTokens.BrandCyan else VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusSmall))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) VlastTokens.DarkBackground else VlastTokens.TextSecondary
        )
    }
}

@Composable
private fun LogItemCard(entry: ActivityLogEntry, formattedTime: String) {
    val (color, icon) = when (entry.eventType) {
        ActivityLogEntry.EventType.DISCONNECTED -> Pair(VlastTokens.SemanticRed, Icons.Filled.Block)
        ActivityLogEntry.EventType.RESTORED -> Pair(VlastTokens.SemanticGreen, Icons.Filled.CheckCircle)
        ActivityLogEntry.EventType.CONFIG_CHANGED -> Pair(VlastTokens.BrandCyan, Icons.Filled.Settings)
        ActivityLogEntry.EventType.SYSTEM_EVENT -> Pair(VlastTokens.BrandAmber, Icons.Filled.AirplaneTicket)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
            .background(VlastTokens.DarkSurface)
            .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusMedium))
            .padding(VlastTokens.Space12)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = entry.description,
                        color = VlastTokens.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedTime,
                    fontFamily = NumberFontFamily,
                    color = VlastTokens.TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
