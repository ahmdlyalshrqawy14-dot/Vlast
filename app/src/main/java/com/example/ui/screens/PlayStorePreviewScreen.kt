package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens

/**
 * Item 32: Google Play Store Preview Showcase Screen.
 * Renders exact store preview screenshots and feature graphic mockups using the precise fonts,
 * color palette, and visual identity of Vlast.
 */
@Composable
fun PlayStorePreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VlastTokens.DarkBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VlastTokens.Space16, vertical = VlastTokens.Space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = VlastTokens.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(VlastTokens.Space8))
            Text(
                text = "معاينة متجر Google Play",
                color = VlastTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(VlastTokens.Space16),
            verticalArrangement = Arrangement.spacedBy(VlastTokens.Space20)
        ) {
            // Feature Graphic (1024x500 banner style)
            item {
                Text(
                    text = "1. البانر التعريفي للمتجر (Feature Graphic)",
                    color = VlastTokens.BrandAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(VlastTokens.Space8))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1024f / 500f)
                        .clip(RoundedCornerShape(VlastTokens.RadiusLarge))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF090D16),
                                    Color(0xFF1E293B)
                                )
                            )
                        )
                        .border(1.5.dp, VlastTokens.BrandCyan, RoundedCornerShape(VlastTokens.RadiusLarge))
                        .padding(VlastTokens.Space16),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(VlastTokens.BrandCyan.copy(alpha = 0.2f))
                                .border(1.5.dp, VlastTokens.BrandCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = VlastTokens.BrandCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "Vlast",
                            fontFamily = NumberFontFamily,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "التحكم المطلق والقطع الحاسِم لبيانات الإنترنت",
                            color = VlastTokens.BrandCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "حظر تلقائي عند الحدود • مراقبة دقيقة • استقلالية محلية 100%",
                            color = VlastTokens.TextMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Screenshots Gallery (App Store Mockups)
            item {
                Text(
                    text = "2. لقطات الشاشة للتطبيق (App Store Screenshots)",
                    color = VlastTokens.BrandCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "مصممة بنسبة العرض للهاتف مع الإطارات التوضيحية لنفس خطوط وألوان التطبيق:",
                    color = VlastTokens.TextMuted,
                    fontSize = 11.sp
                )
            }

            item {
                val mockups = listOf(
                    StoreMockupData(
                        title = "العداد الفوري ذو التدرج اللوني",
                        subtitle = "مراقبة دقيقة تميز استهلاك Wi-Fi وعربات بيانات الهاتف",
                        icon = Icons.Filled.Speed,
                        accentColor = VlastTokens.BrandCyan,
                        contentDetail = "1.45 GB / 2.00 GB (72.5%)"
                    ),
                    StoreMockupData(
                        title = "التحكم بالخدمات الثلاث",
                        subtitle = "الحد الدائم، حد اليوم المؤقت، والقطع الكامل الفوري",
                        icon = Icons.Filled.Block,
                        accentColor = VlastTokens.BrandRed,
                        contentDetail = "شجرة أولوية صارمة تضمن التطبيق التلقائي"
                    ),
                    StoreMockupData(
                        title = "التحكم بالتطبيقات (Per-App)",
                        subtitle = "حظر كامل أو تحديد حد مستقل لكل تطبيق مثبت",
                        icon = Icons.Filled.Tune,
                        accentColor = VlastTokens.BrandAmber,
                        contentDetail = "تطبيقات المحظورة: 3 تطبيقات active"
                    ),
                    StoreMockupData(
                        title = "تقارير التدقيق وسجل الأحداث",
                        subtitle = "سجل رسمي لأسباب القطع والاستئناف بدقة متناهية",
                        icon = Icons.Filled.BarChart,
                        accentColor = VlastTokens.SemanticGreen,
                        contentDetail = "سجل 7 / 30 يوماً مع رسوم بيانية توضيحية"
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(VlastTokens.Space12)
                ) {
                    itemsIndexed(mockups) { index, mockup ->
                        StoreScreenshotMockupCard(
                            screenIndex = index + 1,
                            data = mockup
                        )
                    }
                }
            }
        }
    }
}

private data class StoreMockupData(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color,
    val contentDetail: String
)

@Composable
private fun StoreScreenshotMockupCard(
    screenIndex: Int,
    data: StoreMockupData
) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(VlastTokens.RadiusLarge))
            .background(VlastTokens.DarkSurface)
            .border(1.5.dp, data.accentColor, RoundedCornerShape(VlastTokens.RadiusLarge))
            .padding(VlastTokens.Space12)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VlastTokens.Space4)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                        .background(data.accentColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "الشاشة #$screenIndex",
                        color = data.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = data.title,
                    color = VlastTokens.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = data.subtitle,
                    color = VlastTokens.TextMuted,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
            }

            // Phone UI Representation Inside Mockup Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = VlastTokens.Space8)
                    .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
                    .background(VlastTokens.DarkBackground)
                    .border(1.dp, VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusMedium))
                    .padding(VlastTokens.Space8),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
                ) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = null,
                        tint = data.accentColor,
                        modifier = Modifier.size(32.dp)
                    )

                    Text(
                        text = data.contentDetail,
                        fontFamily = NumberFontFamily,
                        color = VlastTokens.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Footer branding
            Text(
                text = "Vlast • Sovereign Data Control",
                color = VlastTokens.TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
