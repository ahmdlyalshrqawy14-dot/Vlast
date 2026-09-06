package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VlastTokens
import kotlinx.coroutines.delay

/**
 * Dynamic Island Style Greeting Banner (Items 16, 17, 18):
 *
 * Contains 2 independent layers:
 * 1. Animated text layer cycling every 6 seconds through 10 languages.
 *    EXPLICIT EXCEPTION: Sole place where Vlast may be written in the script of the language
 *    (e.g., Arabic "فلاست", Russian "Власть", etc.).
 * 2. Static action button layer that NEVER moves, changes, or disappears:
 *    - Kill switch toggle
 *    - Quick edit today override
 *    - Direct navigation to Reports/Logs
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DynamicIslandHeader(
    isKillSwitchActive: Boolean,
    onToggleKillSwitch: () -> Unit,
    onNavigateToTodayOverride: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 10 authorized languages with localized greeting (Item 16)
    val greetings = remember {
        listOf(
            "مرحبًا بك في فلاست — التحكم الكامل في بياناتك",      // Arabic: فلاست
            "Welcome to Vlast — Master your data limits",          // English: Vlast
            "Добро пожаловать в Власть — Полный контроль данных",  // Russian: Власть
            "Bienvenue sur Vlast — Maîtrisez votre consommation", // French: Vlast
            "Willkommen bei Vlast — Volle Datenkontrolle",        // German: Vlast
            "Bienvenido a Vlast — Control absoluto de datos",     // Spanish: Vlast
            "Benvenuto in Vlast — Pieno controllo dei dati",      // Italian: Vlast
            "欢迎使用弗拉斯特 — 掌控您的网络数据流量",               // Chinese: 弗拉斯特
            "ヴラストへようこそ — データ通信を完全制御",               // Japanese: ヴラスト
            "Bem-vindo ao Vlast — Controle total dos dados"       // Portuguese: Vlast
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(6000L) // Cycles exactly every 6 seconds (Item 16)
            currentIndex = (currentIndex + 1) % greetings.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusLarge))
            .background(VlastTokens.DarkSurfaceVariant)
            .border(1.dp, VlastTokens.DarkBorderActive, RoundedCornerShape(VlastTokens.RadiusLarge))
            .padding(horizontal = VlastTokens.Space16, vertical = VlastTokens.Space12)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Layer 1: Animated Morphing Text Layer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = VlastTokens.Space12),
                contentAlignment = Alignment.CenterStart
            ) {
                AnimatedContent(
                    targetState = greetings[currentIndex],
                    transitionSpec = {
                        fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) togetherWith
                                fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                    },
                    label = "GreetingCycle"
                ) { greetingText ->
                    Text(
                        text = greetingText,
                        color = VlastTokens.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }
            }

            // Layer 2: STRICT STATIC ACTION BUTTONS (Never change or disappear)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
            ) {
                // Button 1: Kill Switch Toggle (High urgency)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                        .background(if (isKillSwitchActive) VlastTokens.BrandRed else VlastTokens.DarkSurface)
                        .border(
                            1.dp,
                            if (isKillSwitchActive) VlastTokens.BrandRedDark else VlastTokens.DarkBorder,
                            RoundedCornerShape(VlastTokens.RadiusSmall)
                        )
                        .clickable { onToggleKillSwitch() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = "القطع الكامل",
                        tint = if (isKillSwitchActive) Color.White else VlastTokens.BrandRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Button 2: Today Override Shortcut
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                        .background(VlastTokens.DarkSurface)
                        .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusSmall))
                        .clickable { onNavigateToTodayOverride() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditCalendar,
                        contentDescription = "تعديل حد اليوم",
                        tint = VlastTokens.BrandAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Button 3: Direct to Reports / Logs
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                        .background(VlastTokens.DarkSurface)
                        .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusSmall))
                        .clickable { onNavigateToReports() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = "التقارير",
                        tint = VlastTokens.BrandCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
