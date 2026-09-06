package com.example.ui.dialogs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
 * Item 28: Signature Moment for Emergency Kill Switch Activation.
 * Dedicated visual interaction with animated pulsing glow, conveying "Decision with Weight".
 */
@Composable
fun KillSwitchSignatureMomentDialog(
    onAcknowledge: () -> Unit,
    onUndo: () -> Unit
) {
    val pulseScale = remember { Animatable(1f) }
    val pulseAlpha = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        pulseScale.animateTo(
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        pulseAlpha.animateTo(
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    AlertDialog(
        onDismissRequest = onAcknowledge,
        containerColor = VlastTokens.DarkSurface,
        shape = RoundedCornerShape(VlastTokens.RadiusLarge),
        modifier = Modifier.border(2.dp, VlastTokens.BrandRed, RoundedCornerShape(VlastTokens.RadiusLarge)),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pulsing Aura Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = VlastTokens.Space8)
                ) {
                    Box(
                        modifier = Modifier
                            .size((64 * pulseScale.value).dp)
                            .clip(CircleShape)
                            .background(VlastTokens.BrandRed.copy(alpha = pulseAlpha.value))
                    )
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(VlastTokens.BrandRed)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(VlastTokens.Space12))

                Text(
                    text = "⚡ قرار حاسم: تم تفعيل القطع الكامل",
                    color = VlastTokens.BrandRed,
                    fontSize = 20.sp,
                    fontFamily = NumberFontFamily,
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
                        .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                        .background(VlastTokens.BrandRedDark)
                        .border(1.dp, VlastTokens.BrandRed, RoundedCornerShape(VlastTokens.RadiusMedium))
                        .padding(VlastTokens.Space12)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "تم تنفيذ الحظر الشامل الفوري لكافة حركات الإنترنت على Wi-Fi وبيانات الهاتف محلياً.",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 17.sp
                        )
                    }
                }

                Text(
                    text = "القطع الكامل يعمل بأعلى أولوية في النظام (1/4) ولا يتأثر بتغير أوقات اليوم حتى تقوم بإلغائه صراحة.",
                    color = VlastTokens.TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAcknowledge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VlastTokens.BrandRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(VlastTokens.RadiusMedium),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "موافق (إبقاء الحظر نشطًا)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onUndo,
                shape = RoundedCornerShape(VlastTokens.RadiusMedium),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تراجع وإلغاء القطع الآن",
                    color = VlastTokens.TextPrimary,
                    fontSize = 12.sp
                )
            }
        }
    )
}
