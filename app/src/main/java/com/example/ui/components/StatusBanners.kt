package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ServicePriorityStatus
import com.example.core.viewmodel.VlastMechanicsViewModel
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens

/**
 * Visual Indicators for Priority, Lock Banner, Undo Snackbar & Confirmations (Items 4, 9, 19, 28)
 */

/**
 * Item 4: Prominent Visual Card for Active Service Priority Resolution
 */
@Composable
fun ServicePriorityBanner(
    status: ServicePriorityStatus,
    modifier: Modifier = Modifier
) {
    val (accentColor, title, description) = when (status) {
        is ServicePriorityStatus.KillSwitchInControl -> Triple(
            VlastTokens.BrandRed,
            "الأولوية القصوى (1/4): القطع الكامل نشط",
            "جميع حدود البيانات الأخرى معطلة مؤقتًا لصالح الحظر الشامل."
        )
        is ServicePriorityStatus.TodayOverrideInControl -> Triple(
            VlastTokens.BrandAmber,
            "الأولوية المطبقة (2/4): حد اليوم المؤقت (${status.networkName})",
            "الحد الفعّال اليوم هو ${status.limitDisplay} (تم تجاوز الحد الدائم لليوم فقط)."
        )
        is ServicePriorityStatus.RecurringLimitInControl -> Triple(
            VlastTokens.BrandCyan,
            "الأولوية المطبقة (3/4): الحد اليومي الدائم (${status.networkName})",
            "الحد اليومي الدائم البالغ ${status.limitDisplay} هو المتحكم في هذا الاتصال."
        )
        is ServicePriorityStatus.UnlimitedInControl -> Triple(
            VlastTokens.SemanticGreen,
            "الأولوية (4/4): استهلاك حر بدون قيود",
            "لا يوجد حد مفعل حاليًا، تدفق البيانات يعمل بحرية تامة."
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
            .background(VlastTokens.DarkSurfaceVariant)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(VlastTokens.RadiusMedium))
            .padding(horizontal = VlastTokens.Space16, vertical = VlastTokens.Space12)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = VlastTokens.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * Item 19: Dedicated Cutoff/Lock Banner with prominent lock icon
 */
@Composable
fun CutoffActiveBanner(
    isKillSwitch: Boolean,
    reasonText: String,
    onQuickRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
            .background(VlastTokens.BrandRedDark)
            .border(1.5.dp, VlastTokens.BrandRed, RoundedCornerShape(VlastTokens.RadiusMedium))
            .padding(VlastTokens.Space16)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "قفل الاتصال",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = if (isKillSwitch) "انقطاع البيانات: القطع الكامل اليدوي" else "تم إيقاف البيانات للوصول للحد",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reasonText,
                        color = VlastTokens.TextPrimary.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            if (isKillSwitch) {
                Button(
                    onClick = onQuickRestore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = VlastTokens.BrandRedDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("استئناف الاتصال وإلغاء القطع الآن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Item 9: 6-Second Rapid Undo Snackbar
 */
@Composable
fun RapidUndoSnackbar(
    undoState: VlastMechanicsViewModel.UndoState?,
    onUndoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = undoState != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (undoState != null) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                    .background(VlastTokens.DarkSurface)
                    .border(1.dp, VlastTokens.BrandCyan, RoundedCornerShape(VlastTokens.RadiusMedium))
                    .padding(horizontal = VlastTokens.Space16, vertical = VlastTokens.Space12)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
                    ) {
                        Text(
                            text = "تم تعديل الحد (${undoState.secondsRemaining} ث)",
                            color = VlastTokens.TextPrimary,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onUndoClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VlastTokens.BrandCyan,
                            contentColor = VlastTokens.DarkBackground
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("تراجع سريع", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item 7 & 22: Branded Confirmation Dialog for Sensitive Actions
 */
@Composable
fun VlastConfirmationDialog(
    request: VlastMechanicsViewModel.ConfirmationRequest?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (request == null) return

    val (title, body) = when (request) {
        is VlastMechanicsViewModel.ConfirmationRequest.EnableKillSwitch -> Pair(
            "تأكيد تفعيل القطع الكامل",
            "أنت على وشك قطع وحظر جميع حركات البيانات على كل من Wi-Fi وبيانات الهاتف فورًا. هل تريد المتابعة؟"
        )
        is VlastMechanicsViewModel.ConfirmationRequest.DecreaseRecurringLimit -> Pair(
            "تأكيد تخفيض الحد الدائم",
            "أنت على وشك تخفيض الحد اليومي الدائم. قد يؤدي ذلك إلى قطع الاتصال فورًا إذا كان استهلاكك الحالي قد تجاوز الحد الجديد."
        )
        is VlastMechanicsViewModel.ConfirmationRequest.EnableAppBlock -> Pair(
            "تأكيد حظر تطبيق ${request.appName}",
            "أنت على وشك حظر ${request.appName} بالكامل من استخدام الإنترنت (Wi-Fi وبيانات الهاتف). لن يتمكن التطبيق من إرسال أو استقبال أي بيانات. هل تريد المتابعة؟"
        )
        is VlastMechanicsViewModel.ConfirmationRequest.DisableAppBlock -> Pair(
            "تأكيد إلغاء حظر تطبيق ${request.appName}",
            "أنت على وشك إلغاء الحظر الكامل عن تطبيق ${request.appName} والسماح له باتصال الإنترنت مجدداً. هل تريد المتابعة؟"
        )
        is VlastMechanicsViewModel.ConfirmationRequest.ShutdownApp -> Pair(
            "تأكيد إيقاف تشغيل Vlast بالكامل",
            "أنت على وشك إيقاف كل وظائف Vlast نهائياً (إيقاف النفق، المراقبة الحية، الإشعارات، ونقطة الاتصال). سيتوقف التطبيق تماماً عن العمل في الخلفية وتتوقف كافة خدماته. هل تريد المتابعة؟"
        )
        is VlastMechanicsViewModel.ConfirmationRequest.FactoryResetApp -> Pair(
            "تأكيد إعادة التعيين الشاملة للتطبيق",
            "تحذير سيادي: سيتم مسح كافة البيانات، السجلات، الحدود اليومية، وقواعد التحكم بالتطبيقات نهائياً وإعادة التطبيق لحالته المصنعية الأولى كأنه ثُبّت للتو. هل أنت متأكد تماماً من هذه الإعادة الشاملة؟"
        )
        is VlastMechanicsViewModel.ConfirmationRequest.StopProtectionVpn -> Pair(
            "تأكيد إيقاف الـ VPN والحماية",
            "أنت على وشك إيقاف نفق الـ VPN والفلترة المباشرة مؤقتاً. سيعود اتصال الإنترنت للعمل بشكل طبيعي ودون أي تدخل أو قيود من Vlast. هل تريد المتابعة؟"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VlastTokens.DarkSurface,
        titleContentColor = VlastTokens.TextPrimary,
        textContentColor = VlastTokens.TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = VlastTokens.BrandAmber)
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(text = body, fontSize = 13.sp, lineHeight = 18.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = VlastTokens.BrandRed)
            ) {
                Text("تأكيد الإجراء", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VlastTokens.TextSecondary)
            ) {
                Text("إلغاء")
            }
        }
    )
}
