package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.SmartUnitFormatter
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens

/**
 * Limit Config Input Dialog:
 * - Strict numeric keyboard (Item 20)
 * - Quick Unit Switcher (MB / GB) (Item 6)
 * - Preset quick-choice buttons (500 MB, 1 GB, 2 GB, 5 GB)
 */
@Composable
fun LimitInputDialog(
    title: String,
    initialBytes: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialUnit = if (initialBytes != null && initialBytes >= 1024L * 1024L * 1024L) "GB" else "MB"
    val initialAmount = if (initialBytes != null && initialBytes > 0L) {
        val f = SmartUnitFormatter.format(initialBytes)
        f.amount
    } else {
        "1"
    }

    var textInput by remember { mutableStateOf(initialAmount) }
    var selectedUnit by remember { mutableStateOf(initialUnit) }

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
                Icon(Icons.Filled.Tune, contentDescription = null, tint = VlastTokens.BrandCyan)
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
            ) {
                Text(
                    text = "حدد سقف استهلاك البيانات المطلوب بدقة:",
                    fontSize = 12.sp,
                    color = VlastTokens.TextMuted
                )

                // Input field + Unit selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { newValue ->
                            // Strictly numbers and decimal point only (Item 20)
                            if (newValue.all { it.isDigit() || it == '.' }) {
                                textInput = newValue
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VlastTokens.BrandCyan,
                            unfocusedBorderColor = VlastTokens.DarkBorder,
                            focusedTextColor = VlastTokens.TextPrimary,
                            unfocusedTextColor = VlastTokens.TextPrimary
                        )
                    )

                    // Unit Toggle Buttons
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                            .background(VlastTokens.DarkSurfaceVariant)
                            .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusSmall))
                    ) {
                        listOf("MB", "GB").forEach { unit ->
                            val isSelected = selectedUnit == unit
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                                    .background(if (isSelected) VlastTokens.BrandCyan else Color.Transparent)
                                    .clickable { selectedUnit = unit }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = unit,
                                    fontFamily = NumberFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) VlastTokens.DarkBackground else VlastTokens.TextPrimary
                                )
                            }
                        }
                    }
                }

                // Quick choice chips
                Text(text = "اختيارات سريعة:", fontSize = 11.sp, color = VlastTokens.TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("500 MB", "1 GB", "2 GB", "5 GB").forEach { preset ->
                        val parts = preset.split(" ")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                                .background(VlastTokens.DarkSurfaceVariant)
                                .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusSmall))
                                .clickable {
                                    textInput = parts[0]
                                    selectedUnit = parts[1]
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                fontFamily = NumberFontFamily,
                                fontSize = 11.sp,
                                color = VlastTokens.TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bytes = SmartUnitFormatter.parseInputToBytes(textInput, selectedUnit)
                    if (bytes != null && bytes > 0L) {
                        onConfirm(bytes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VlastTokens.BrandCyan)
            ) {
                Text("حفظ وتطبيق", fontWeight = FontWeight.Bold, color = VlastTokens.DarkBackground)
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
