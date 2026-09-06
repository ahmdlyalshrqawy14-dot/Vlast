package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VlastTokens

/**
 * Item 21 & 23: Custom Designed Brand Illustrations
 *
 * Dedicated custom vector artwork (Zero stock icons):
 * - Shield with glowing energy core for Empty State (First Day)
 * - Walkthrough shield composition
 */

@Composable
fun VlastEmptyStateIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(120.dp)) {
        val w = size.width
        val h = size.height

        // Draw Shield Outline
        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            cubicTo(w * 0.85f, h * 0.05f, w * 0.95f, h * 0.35f, w * 0.95f, h * 0.55f)
            cubicTo(w * 0.95f, h * 0.82f, w * 0.5f, h * 0.98f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.5f, h * 0.98f, w * 0.05f, h * 0.82f, w * 0.05f, h * 0.55f)
            cubicTo(w * 0.05f, h * 0.35f, w * 0.15f, h * 0.05f, w * 0.5f, h * 0.05f)
            close()
        }

        // Deep background shield
        drawPath(path = shieldPath, color = Color(0xFF0F172A))
        // Stroke border
        drawPath(
            path = shieldPath,
            color = Color(0xFF0EA5E9),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Center Pulse Wave
        drawCircle(
            color = Color(0xFF0EA5E9).copy(alpha = 0.25f),
            radius = w * 0.22f,
            center = Offset(w * 0.5f, h * 0.52f)
        )
        drawCircle(
            color = Color(0xFFE11D48),
            radius = w * 0.08f,
            center = Offset(w * 0.5f, h * 0.52f)
        )

        // Pulse line across
        drawLine(
            color = Color(0xFFF8FAFC),
            start = Offset(w * 0.35f, h * 0.52f),
            end = Offset(w * 0.65f, h * 0.52f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * First-Day Empty State View (Item 21 & 23)
 */
@Composable
fun FirstDayEmptyStateCard(
    onConfigureLimits: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusLarge))
            .background(VlastTokens.DarkSurface)
            .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusLarge))
            .padding(VlastTokens.Space24)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
        ) {
            VlastEmptyStateIllustration()

            Text(
                text = "بداية موفقة مع Vlast",
                color = VlastTokens.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "لم يتم تسجيل أي استهلاك بيانات حتى الآن اليوم. نظام المراقبة والحماية يعمل في الخلفية وجاهز لفرض حدودك فور بدء استخدام الإنترنت.",
                color = VlastTokens.TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = VlastTokens.Space8)
            )

            Button(
                onClick = onConfigureLimits,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VlastTokens.BrandCyan,
                    contentColor = VlastTokens.DarkBackground
                ),
                modifier = Modifier.padding(top = VlastTokens.Space4)
            ) {
                Text("ضبط حدود اليوم والحد الدائم", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
