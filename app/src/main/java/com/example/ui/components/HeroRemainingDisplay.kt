package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.MeterProgressStatus
import com.example.core.model.SmartUnitFormatter
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens

/**
 * Primary Hero Counter (Item 11, 1, 5, 6, 21):
 * - Largest element on screen: Prominent "REMAINING" number display
 * - Strictly Monospace numbers to eliminate numerical jitter (Item 7)
 * - Semantic tri-color gradation (Green / Yellow / Red) (Item 4)
 * - Color-blind friendly companion indicator shapes (Item 5)
 * - Tap-to-copy capability (Item 21)
 */
@Composable
fun HeroRemainingDisplay(
    networkLabel: String,
    remainingBytes: Long?,
    usedBytes: Long,
    effectiveLimitBytes: Long?,
    status: MeterProgressStatus,
    onCopyValue: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val progressColor = when (status) {
        MeterProgressStatus.GREEN -> VlastTokens.SemanticGreen
        MeterProgressStatus.YELLOW -> VlastTokens.SemanticYellow
        MeterProgressStatus.RED -> VlastTokens.SemanticRed
    }

    // Color-blind friendly companion icon & pattern descriptor (Item 5)
    val (statusIcon, statusShapeLabel) = when (status) {
        MeterProgressStatus.GREEN -> Pair(Icons.Filled.CheckCircle, "● آمن (أقل من 70%)")
        MeterProgressStatus.YELLOW -> Pair(Icons.Filled.Warning, "▲ تحذير (70% - 90%)")
        MeterProgressStatus.RED -> Pair(Icons.Filled.Info, "■ حرج / قطع (أكثر من 90%)")
    }

    val remainingDisplay = if (remainingBytes != null) {
        SmartUnitFormatter.format(remainingBytes)
    } else {
        SmartUnitFormatter.FormattedValue("∞", "", 0L)
    }

    val usedFormatted = SmartUnitFormatter.formatDisplay(usedBytes)
    val limitFormatted = if (effectiveLimitBytes != null) SmartUnitFormatter.formatDisplay(effectiveLimitBytes) else "غير محدود"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusLarge))
            .background(VlastTokens.DarkSurface)
            .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusLarge))
            .padding(VlastTokens.Space20)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Network Badge & Color-blind Shape Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Network Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                        .background(VlastTokens.DarkSurfaceVariant)
                        .padding(horizontal = VlastTokens.Space8, vertical = VlastTokens.Space4)
                ) {
                    Text(
                        text = networkLabel,
                        color = VlastTokens.BrandCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Color-blind shape indicator (Item 5)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space4)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusShapeLabel,
                        tint = progressColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = statusShapeLabel,
                        color = progressColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Central Ring Gauge & Giant Number
            Box(
                modifier = Modifier
                    .padding(vertical = VlastTokens.Space16)
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                val fraction = if (effectiveLimitBytes != null && effectiveLimitBytes > 0L) {
                    (usedBytes.toFloat() / effectiveLimitBytes.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                Canvas(modifier = Modifier.size(190.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    // Track background
                    drawArc(
                        color = Color(0xFF1E293B),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // Progress arc
                    if (fraction > 0f) {
                        drawArc(
                            color = progressColor,
                            startAngle = 135f,
                            sweepAngle = 270f * fraction,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Centered Mega Number
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onCopyValue("المتبقي", "${remainingDisplay.amount} ${remainingDisplay.unit}")
                    }
                ) {
                    Text(
                        text = "المتبقي اليوم",
                        color = VlastTokens.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = remainingDisplay.amount,
                            fontFamily = NumberFontFamily,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = VlastTokens.TextPrimary
                        )
                        if (remainingDisplay.unit.isNotEmpty()) {
                            Text(
                                text = " ${remainingDisplay.unit}",
                                fontFamily = NumberFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VlastTokens.BrandAmber,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    // Copy icon hint
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "نسخ",
                            tint = VlastTokens.TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "اضغط للنسخ",
                            fontSize = 10.sp,
                            color = VlastTokens.TextMuted
                        )
                    }
                }
            }

            // Footer row: Used vs Limit comparison
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                    .background(VlastTokens.DarkSurfaceVariant)
                    .padding(horizontal = VlastTokens.Space12, vertical = VlastTokens.Space8),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "المستهلك:", color = VlastTokens.TextMuted, fontSize = 11.sp)
                    Text(
                        text = usedFormatted,
                        fontFamily = NumberFontFamily,
                        color = VlastTokens.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "السقف الفعّال:", color = VlastTokens.TextMuted, fontSize = 11.sp)
                    Text(
                        text = limitFormatted,
                        fontFamily = NumberFontFamily,
                        color = VlastTokens.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
