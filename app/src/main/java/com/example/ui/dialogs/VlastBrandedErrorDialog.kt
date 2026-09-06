package com.example.ui.dialogs

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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens

/**
 * Item 22: Custom Branded Error Modal for VPN Permission Denied or Revocation.
 * Styled completely in Vlast's dark sovereign visual theme rather than generic system alerts.
 */
@Composable
fun VlastBrandedErrorDialog(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VlastTokens.DarkSurface,
        shape = RoundedCornerShape(VlastTokens.RadiusLarge),
        modifier = Modifier
            .border(1.5.dp, VlastTokens.BrandRed, RoundedCornerShape(VlastTokens.RadiusLarge)),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(VlastTokens.BrandRed.copy(alpha = 0.15f))
                        .border(1.dp, VlastTokens.BrandRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = VlastTokens.BrandRed,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(VlastTokens.Space12))

                Text(
                    text = title,
                    color = VlastTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                        .background(VlastTokens.DarkSurfaceVariant)
                        .border(1.dp, VlastTokens.BrandAmber.copy(alpha = 0.3f), RoundedCornerShape(VlastTokens.RadiusSmall))
                        .padding(VlastTokens.Space12)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = VlastTokens.BrandAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = message,
                            color = VlastTokens.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Text(
                    text = "ملاحظة: Vlast يعتمد على نفل الفلترة المحلي داخل جهازك 100% دون أي اتصال بخوادم خارجية لحساب الاستهلاك بدقة متناهية وفرض الحدود فوراً.",
                    color = VlastTokens.TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VlastTokens.BrandCyan,
                    contentColor = VlastTokens.DarkBackground
                ),
                shape = RoundedCornerShape(VlastTokens.RadiusMedium),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "منح / إعادة تفعيل الحماية والفلترة الآن",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(VlastTokens.RadiusMedium),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "إغلاق",
                    color = VlastTokens.TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    )
}
