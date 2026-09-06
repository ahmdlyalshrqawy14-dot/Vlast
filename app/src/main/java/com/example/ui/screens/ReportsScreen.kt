package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplaneTicket
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.model.ActivityLogEntry
import com.example.core.model.DailyUsageRecord
import com.example.core.model.SmartUnitFormatter
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen 2: Activity Logs & Historical Reports (Items 10, 11, 13, 17 & Section 3):
 * Includes Sub-tabs:
 * 1. Activity Log (Audit trail of disconnections/restorations)
 * 2. Historical Charts (Wi-Fi vs Mobile consumption over 7 or 30 days)
 */
@Composable
fun ReportsScreen(
    logs: List<ActivityLogEntry>,
    historicalRecords: List<DailyUsageRecord> = emptyList(),
    selectedRangeDays: Int = 7,
    onSelectRangeDays: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Logs, 1: Charts
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
        // Sub-tabs row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = VlastTokens.Space8, bottom = VlastTokens.Space12)
                .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                .background(VlastTokens.DarkSurface)
                .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusMedium))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SubTabButton(
                title = stringResource(R.string.tab_activity_logs),
                icon = Icons.Filled.List,
                isSelected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                modifier = Modifier.weight(1f)
            )
            SubTabButton(
                title = stringResource(R.string.tab_historical_charts),
                icon = Icons.Filled.BarChart,
                isSelected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        if (selectedSubTab == 0) {
            // Tab 1: Activity Log
            Text(
                text = stringResource(R.string.reports_title),
                color = VlastTokens.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = VlastTokens.Space4)
            )
            Text(
                text = stringResource(R.string.reports_subtitle),
                color = VlastTokens.TextMuted,
                fontSize = 11.sp,
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
                    label = "${stringResource(R.string.filter_all)} (${logs.size})",
                    isSelected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                FilterPill(
                    label = stringResource(R.string.filter_cut),
                    isSelected = selectedFilter == ActivityLogEntry.EventType.DISCONNECTED,
                    onClick = { selectedFilter = ActivityLogEntry.EventType.DISCONNECTED }
                )
                FilterPill(
                    label = stringResource(R.string.filter_restored),
                    isSelected = selectedFilter == ActivityLogEntry.EventType.RESTORED,
                    onClick = { selectedFilter = ActivityLogEntry.EventType.RESTORED }
                )
                FilterPill(
                    label = stringResource(R.string.filter_system),
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
        } else {
            // Tab 2: Historical Charts (Section 3)
            HistoricalChartsContent(
                records = historicalRecords,
                selectedRangeDays = selectedRangeDays,
                onSelectRangeDays = onSelectRangeDays,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SubTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
            .background(if (isSelected) VlastTokens.BrandCyan.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) VlastTokens.BrandCyan.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(VlastTokens.RadiusSmall)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) VlastTokens.BrandCyan else VlastTokens.TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                color = if (isSelected) VlastTokens.BrandCyan else VlastTokens.TextMuted,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun HistoricalChartsContent(
    records: List<DailyUsageRecord>,
    selectedRangeDays: Int,
    onSelectRangeDays: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalWifi = remember(records) { records.sumOf { it.wifiUsedBytes } }
    val totalMobile = remember(records) { records.sumOf { it.mobileUsedBytes + it.sim2UsedBytes } }
    val totalCombined = totalWifi + totalMobile

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Range selector: 7 days vs 30 days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سجل استهلاك الأجهزة",
                color = VlastTokens.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterPill(
                    label = stringResource(R.string.history_7_days),
                    isSelected = selectedRangeDays == 7,
                    onClick = { onSelectRangeDays(7) }
                )
                FilterPill(
                    label = stringResource(R.string.history_30_days),
                    isSelected = selectedRangeDays == 30,
                    onClick = { onSelectRangeDays(30) }
                )
            }
        }

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                label = stringResource(R.string.history_total_wifi),
                value = SmartUnitFormatter.formatDisplay(totalWifi),
                accentColor = VlastTokens.BrandCyan,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = stringResource(R.string.history_total_mobile),
                value = SmartUnitFormatter.formatDisplay(totalMobile),
                accentColor = VlastTokens.BrandAmber,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = stringResource(R.string.history_total_period),
                value = SmartUnitFormatter.formatDisplay(totalCombined),
                accentColor = VlastTokens.BrandRed,
                modifier = Modifier.weight(1f)
            )
        }

        // Chart Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                .background(VlastTokens.DarkSurface)
                .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusMedium))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(label = stringResource(R.string.wifi_label), color = VlastTokens.BrandCyan)
                    Spacer(Modifier.width(12.dp))
                    LegendItem(label = stringResource(R.string.mobile_label), color = VlastTokens.BrandAmber)
                }

                // Canvas Bar Chart
                if (records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.history_no_data),
                            color = VlastTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    UsageBarChart(records = records, modifier = Modifier.fillMaxWidth().height(140.dp))
                }
            }
        }

        // Breakdown List Header
        Text(
            text = "التفاصيل اليومية للفترة",
            color = VlastTokens.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        // Scannable Daily Breakdown List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(records) { record ->
                DailyHistoryRow(record = record)
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
            .background(VlastTokens.DarkSurface)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(VlastTokens.RadiusSmall))
            .padding(8.dp)
    ) {
        Column {
            Text(text = label, color = VlastTokens.TextMuted, fontSize = 10.sp)
            Text(
                text = value,
                fontFamily = NumberFontFamily,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(text = label, color = VlastTokens.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun UsageBarChart(records: List<DailyUsageRecord>, modifier: Modifier = Modifier) {
    val wifiColor = VlastTokens.BrandCyan
    val mobileColor = VlastTokens.BrandAmber

    val maxVal = remember(records) {
        val calculated = records.maxOfOrNull { it.wifiUsedBytes + it.mobileUsedBytes + it.sim2UsedBytes } ?: 1L
        if (calculated <= 0L) 1024L else calculated
    }

    Canvas(modifier = modifier) {
        val count = records.size.coerceAtLeast(1)
        val spacing = 8.dp.toPx()
        val totalSpacing = spacing * (count - 1)
        val barWidth = ((size.width - totalSpacing) / count).coerceIn(4.dp.toPx(), 28.dp.toPx())
        val chartHeight = size.height - 18.dp.toPx()

        for (i in records.indices) {
            val rec = records[i]
            val mobileBytes = rec.mobileUsedBytes + rec.sim2UsedBytes
            val wifiBytes = rec.wifiUsedBytes
            val totalBytes = mobileBytes + wifiBytes

            val x = i * (barWidth + spacing)

            if (totalBytes > 0L) {
                val totalFraction = (totalBytes.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1.0f)
                val totalBarHeight = totalFraction * chartHeight

                val mobileFraction = (mobileBytes.toFloat() / totalBytes.toFloat())
                val mobileHeight = totalBarHeight * mobileFraction
                val wifiHeight = totalBarHeight - mobileHeight

                // Draw Mobile Bar (Bottom portion)
                if (mobileHeight > 0f) {
                    drawRoundRect(
                        color = mobileColor,
                        topLeft = Offset(x, chartHeight - mobileHeight),
                        size = Size(barWidth, mobileHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                // Draw Wi-Fi Bar (Top portion)
                if (wifiHeight > 0f) {
                    drawRoundRect(
                        color = wifiColor,
                        topLeft = Offset(x, chartHeight - totalBarHeight),
                        size = Size(barWidth, wifiHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            } else {
                // Baseline placeholder tick
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(x, chartHeight - 2.dp.toPx()),
                    size = Size(barWidth, 2.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun DailyHistoryRow(record: DailyUsageRecord) {
    val wifiText = SmartUnitFormatter.formatDisplay(record.wifiUsedBytes)
    val mobileBytes = record.mobileUsedBytes + record.sim2UsedBytes
    val mobileText = SmartUnitFormatter.formatDisplay(mobileBytes)
    val totalText = SmartUnitFormatter.formatDisplay(record.wifiUsedBytes + mobileBytes)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
            .background(VlastTokens.DarkSurface)
            .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusSmall))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.date,
                    fontFamily = NumberFontFamily,
                    color = VlastTokens.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "واي فاي: $wifiText  •  موبايل: $mobileText",
                    color = VlastTokens.TextMuted,
                    fontSize = 10.sp
                )
            }

            Text(
                text = totalText,
                fontFamily = NumberFontFamily,
                color = VlastTokens.BrandCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
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
            .clip(RoundedCornerShape(VlastTokens.RadiusPill))
            .background(if (isSelected) VlastTokens.BrandCyan else VlastTokens.DarkSurface)
            .border(
                1.dp,
                if (isSelected) VlastTokens.BrandCyan else VlastTokens.DarkBorder,
                RoundedCornerShape(VlastTokens.RadiusPill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) VlastTokens.DarkBackground else VlastTokens.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun LogItemCard(
    entry: ActivityLogEntry,
    formattedTime: String
) {
    val (icon, iconColor) = when (entry.eventType) {
        ActivityLogEntry.EventType.DISCONNECTED -> Icons.Filled.Block to VlastTokens.SemanticRed
        ActivityLogEntry.EventType.RESTORED -> Icons.Filled.CheckCircle to VlastTokens.SemanticGreen
        ActivityLogEntry.EventType.CONFIG_CHANGED -> Icons.Filled.Settings to VlastTokens.BrandCyan
        ActivityLogEntry.EventType.SYSTEM_EVENT -> Icons.Filled.Info to VlastTokens.BrandAmber
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
                    .background(iconColor.copy(alpha = 0.15f))
                    .border(1.dp, iconColor.copy(alpha = 0.3f), RoundedCornerShape(VlastTokens.RadiusSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.specificReason.titleAr,
                        color = VlastTokens.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedTime,
                        fontFamily = NumberFontFamily,
                        color = VlastTokens.TextMuted,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = entry.description,
                    color = VlastTokens.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
