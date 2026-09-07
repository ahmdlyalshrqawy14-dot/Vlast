package com.example.ui.screens

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.core.model.ManagedAppRule
import com.example.core.model.NetworkType
import com.example.core.model.SmartUnitFormatter
import com.example.ui.theme.NumberFontFamily
import com.example.ui.theme.VlastTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val iconBitmap: ImageBitmap?,
    val isSystemApp: Boolean
)

/**
 * Section 6: Dedicated Per-App Control Screen ("التحكم بالتطبيقات").
 *
 * Requirements:
 * 1) Searchable list of installed apps with real names and icons.
 * 2) Independent control card per selected app:
 *    - Full block switch (with long-press/confirmation mechanism).
 *    - Custom daily limit button (using LimitInputDialog).
 *    - Current usage display with semantic color meter (Green / Yellow / Red).
 * 3) Apps with active rules pinned at top with distinct badge.
 * 4) Android < 10 fallback banner when accessed on unsupported OS versions.
 */
@Composable
fun AppControlScreen(
    appRules: List<ManagedAppRule>,
    onRequestBlockToggle: (packageName: String, appName: String, targetBlocked: Boolean) -> Unit,
    onOpenSetAppNetworkLimit: (packageName: String, appName: String, networkType: NetworkType, simSlot: Int, currentLimitBytes: Long?) -> Unit,
    onDisableAppNetworkLimit: (packageName: String, appName: String, networkType: NetworkType, simSlot: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Android < 10 check
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        UnsupportedAndroidVersionView(onBack = onBack, modifier = modifier)
        return
    }

    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedPackageName by remember { mutableStateOf<String?>(null) }

    // Load installed applications in background
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherActivities = try {
                pm.queryIntentActivities(intent, 0)
            } catch (_: Throwable) {
                emptyList()
            }
            val seen = HashSet<String>()
            val list = mutableListOf<InstalledAppItem>()

            for (resolveInfo in launcherActivities) {
                val pkg = resolveInfo.activityInfo?.packageName ?: continue
                if (pkg == context.packageName) continue // Skip Vlast itself
                if (seen.add(pkg)) {
                    val label = resolveInfo.loadLabel(pm)?.toString() ?: pkg
                    val iconBitmap = try {
                        resolveInfo.loadIcon(pm)?.toBitmap(96, 96)?.asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                    val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    list.add(InstalledAppItem(pkg, label, iconBitmap, isSystem))
                }
            }

            // Also include any previously managed apps that may not have a launcher activity
            for (rule in appRules) {
                if (rule.packageName != context.packageName && seen.add(rule.packageName)) {
                    val appInfo = try {
                        pm.getApplicationInfo(rule.packageName, 0)
                    } catch (_: Exception) {
                        null
                    }
                    val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: rule.appDisplayName
                    val iconBitmap = appInfo?.let {
                        try {
                            pm.getApplicationIcon(it).toBitmap(96, 96).asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    val isSystem = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false
                    list.add(InstalledAppItem(rule.packageName, label, iconBitmap, isSystem))
                }
            }

            list.sortBy { it.appName.lowercase() }
            withContext(Dispatchers.Main) {
                installedApps = list
                isLoadingApps = false
            }
        }
    }

    val rulesMap = remember(appRules) {
        appRules.associateBy { it.packageName }
    }

    // Filter by search query
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            val q = searchQuery.trim().lowercase()
            installedApps.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    // Split into pinned (active rules) and regular apps
    val pinnedApps = remember(filteredApps, rulesMap) {
        filteredApps.filter { app ->
            val rule = rulesMap[app.packageName]
            rule != null && rule.hasActiveRule
        }
    }

    val regularApps = remember(filteredApps, rulesMap) {
        filteredApps.filter { app ->
            val rule = rulesMap[app.packageName]
            rule == null || !rule.hasActiveRule
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VlastTokens.DarkBackground)
            .padding(VlastTokens.Space16)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = VlastTokens.Space12),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "التحكم بالتطبيقات",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VlastTokens.TextPrimary
                )
                Text(
                    text = "حظر كامل أو وضع حد يومي مخصص لكل تطبيق",
                    fontSize = 11.sp,
                    color = VlastTokens.TextMuted
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث عن تطبيق بالاسم أو الحزمة...", fontSize = 13.sp, color = VlastTokens.TextMuted) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = VlastTokens.BrandCyan)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "مسح", tint = VlastTokens.TextMuted)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = VlastTokens.Space12),
            shape = RoundedCornerShape(VlastTokens.RadiusMedium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VlastTokens.BrandCyan,
                unfocusedBorderColor = VlastTokens.DarkBorder,
                focusedContainerColor = VlastTokens.DarkSurface,
                unfocusedContainerColor = VlastTokens.DarkSurface,
                focusedTextColor = VlastTokens.TextPrimary,
                unfocusedTextColor = VlastTokens.TextPrimary
            )
        )

        if (isLoadingApps) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = VlastTokens.BrandCyan)
                    Spacer(modifier = Modifier.height(VlastTokens.Space12))
                    Text(
                        text = "جاري قراءة التطبيقات المثبتة...",
                        color = VlastTokens.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لم يتم العثور على أي تطبيق يطابق البحث.",
                    color = VlastTokens.TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(VlastTokens.Space8)
            ) {
                // Section 1: Pinned Apps (with active rules)
                if (pinnedApps.isNotEmpty()) {
                    item(key = "header_pinned") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = VlastTokens.Space4)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = null,
                                tint = VlastTokens.BrandAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تطبيقات بقواعد نشطة (${pinnedApps.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = VlastTokens.BrandAmber
                            )
                        }
                    }

                    items(items = pinnedApps, key = { "pinned_${it.packageName}" }) { app ->
                        val rule = rulesMap[app.packageName]
                        AppRuleCard(
                            app = app,
                            rule = rule,
                            isExpanded = expandedPackageName == app.packageName,
                            isPinned = true,
                            onCardClick = {
                                expandedPackageName = if (expandedPackageName == app.packageName) null else app.packageName
                            },
                            onRequestBlockToggle = { onRequestBlockToggle(app.packageName, app.appName, it) },
                            onOpenSetNetworkLimit = { netType, simSlot, currentLimit ->
                                onOpenSetAppNetworkLimit(app.packageName, app.appName, netType, simSlot, currentLimit)
                            },
                            onDisableNetworkLimit = { netType, simSlot ->
                                onDisableAppNetworkLimit(app.packageName, app.appName, netType, simSlot)
                            }
                        )
                    }

                    item(key = "divider_regular") {
                        Spacer(modifier = Modifier.height(VlastTokens.Space8))
                        Text(
                            text = "جميع التطبيقات المثبتة (${regularApps.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = VlastTokens.TextSecondary,
                            modifier = Modifier.padding(vertical = VlastTokens.Space4)
                        )
                    }
                }

                // Section 2: Regular Apps
                items(items = regularApps, key = { "regular_${it.packageName}" }) { app ->
                    val rule = rulesMap[app.packageName]
                    AppRuleCard(
                        app = app,
                        rule = rule,
                        isExpanded = expandedPackageName == app.packageName,
                        isPinned = false,
                        onCardClick = {
                            expandedPackageName = if (expandedPackageName == app.packageName) null else app.packageName
                        },
                        onRequestBlockToggle = { onRequestBlockToggle(app.packageName, app.appName, it) },
                        onOpenSetNetworkLimit = { netType, simSlot, currentLimit ->
                            onOpenSetAppNetworkLimit(app.packageName, app.appName, netType, simSlot, currentLimit)
                        },
                        onDisableNetworkLimit = { netType, simSlot ->
                            onDisableAppNetworkLimit(app.packageName, app.appName, netType, simSlot)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRuleCard(
    app: InstalledAppItem,
    rule: ManagedAppRule?,
    isExpanded: Boolean,
    isPinned: Boolean,
    onCardClick: () -> Unit,
    onRequestBlockToggle: (Boolean) -> Unit,
    onOpenSetNetworkLimit: (networkType: NetworkType, simSlot: Int, currentLimitBytes: Long?) -> Unit,
    onDisableNetworkLimit: (networkType: NetworkType, simSlot: Int) -> Unit
) {
    val isBlocked = rule?.isFullyBlocked == true
    val wifiLimit = rule?.wifiDailyLimitBytes
    val wifiEnabled = rule?.wifiDailyLimitEnabled == true && (wifiLimit ?: 0L) > 0L
    val wifiUsed = rule?.wifiUsedBytesToday ?: 0L

    val sim1Limit = rule?.sim1DailyLimitBytes
    val sim1Enabled = rule?.sim1DailyLimitEnabled == true && (sim1Limit ?: 0L) > 0L
    val sim1Used = rule?.sim1UsedBytesToday ?: 0L

    val sim2Limit = rule?.sim2DailyLimitBytes
    val sim2Enabled = rule?.sim2DailyLimitEnabled == true && (sim2Limit ?: 0L) > 0L
    val sim2Used = rule?.sim2UsedBytesToday ?: 0L

    val hasAnyLimit = wifiEnabled || sim1Enabled || sim2Enabled

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
            .background(VlastTokens.DarkSurface)
            .border(
                width = 1.dp,
                color = when {
                    isBlocked -> VlastTokens.SemanticRed.copy(alpha = 0.5f)
                    rule?.isLimitReached == true -> VlastTokens.SemanticRed.copy(alpha = 0.5f)
                    isPinned -> VlastTokens.BrandAmber.copy(alpha = 0.35f)
                    else -> VlastTokens.DarkBorder
                },
                shape = RoundedCornerShape(VlastTokens.RadiusMedium)
            )
            .animateContentSize()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCardClick)
                    .padding(VlastTokens.Space12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                            .background(VlastTokens.DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = null,
                            tint = VlastTokens.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(VlastTokens.Space12))

                // App Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = VlastTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(VlastTokens.BrandAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "قاعدة مفعّلة",
                                    fontSize = 9.sp,
                                    color = VlastTokens.BrandAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Header Summary Subtitle
                    if (isBlocked) {
                        Text(
                            text = "محظور بالكامل",
                            fontSize = 11.sp,
                            color = VlastTokens.SemanticRed,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (hasAnyLimit) {
                        val activeSummaryParts = mutableListOf<String>()
                        if (wifiEnabled && wifiLimit != null) {
                            val fmt = SmartUnitFormatter.format(wifiLimit)
                            activeSummaryParts.add("واي فاي: ${fmt.amount}${fmt.unit}")
                        }
                        if (sim1Enabled && sim1Limit != null) {
                            val fmt = SmartUnitFormatter.format(sim1Limit)
                            activeSummaryParts.add("شريحة 1: ${fmt.amount}${fmt.unit}")
                        }
                        if (sim2Enabled && sim2Limit != null) {
                            val fmt = SmartUnitFormatter.format(sim2Limit)
                            activeSummaryParts.add("شريحة 2: ${fmt.amount}${fmt.unit}")
                        }
                        Text(
                            text = activeSummaryParts.joinToString(" | "),
                            fontSize = 11.sp,
                            color = VlastTokens.BrandCyan,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        val totalUsed = wifiUsed + sim1Used + sim2Used
                        val usedFmt = SmartUnitFormatter.format(totalUsed)
                        Text(
                            text = "استهلاك اليوم: ${usedFmt.amount} ${usedFmt.unit}",
                            fontSize = 11.sp,
                            color = VlastTokens.TextMuted
                        )
                    }
                }

                // Quick visual status icon
                if (isBlocked) {
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = "محظور",
                        tint = VlastTokens.SemanticRed,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (rule?.isLimitReached == true) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "تجاوز الحد",
                        tint = VlastTokens.SemanticRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Control Card
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VlastTokens.DarkSurfaceVariant.copy(alpha = 0.5f))
                        .padding(VlastTokens.Space12)
                ) {
                    // 1) Full Block Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حظر اتصال التطبيق بالكامل",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBlocked) VlastTokens.SemanticRed else VlastTokens.TextPrimary
                            )
                            Text(
                                text = "قطع جميع حركات البيانات الصادرة والواردة لهذا التطبيق (يتطلب تأكيدًا)",
                                fontSize = 11.sp,
                                color = VlastTokens.TextMuted,
                                lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = isBlocked,
                            onCheckedChange = onRequestBlockToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = VlastTokens.SemanticRed,
                                uncheckedThumbColor = VlastTokens.TextMuted,
                                uncheckedTrackColor = VlastTokens.DarkBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(VlastTokens.Space12))

                    // 2) Per-Network Daily Limits Section Header
                    Text(
                        text = "الحدود اليومية المستقلة حسب الشبكة:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VlastTokens.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(VlastTokens.Space8))

                    // Wi-Fi Limit Card
                    AppNetworkLimitRow(
                        networkTitle = "الواي فاي (Wi-Fi)",
                        limitBytes = wifiLimit,
                        isEnabled = wifiEnabled,
                        usedBytes = wifiUsed,
                        onOpenSet = { onOpenSetNetworkLimit(NetworkType.WIFI, 0, wifiLimit) },
                        onDisable = { onDisableNetworkLimit(NetworkType.WIFI, 0) }
                    )

                    Spacer(modifier = Modifier.height(VlastTokens.Space8))

                    // SIM 1 Limit Card
                    AppNetworkLimitRow(
                        networkTitle = "الشريحة 1 (SIM 1)",
                        limitBytes = sim1Limit,
                        isEnabled = sim1Enabled,
                        usedBytes = sim1Used,
                        onOpenSet = { onOpenSetNetworkLimit(NetworkType.MOBILE, 0, sim1Limit) },
                        onDisable = { onDisableNetworkLimit(NetworkType.MOBILE, 0) }
                    )

                    Spacer(modifier = Modifier.height(VlastTokens.Space8))

                    // SIM 2 Limit Card
                    AppNetworkLimitRow(
                        networkTitle = "الشريحة 2 (SIM 2)",
                        limitBytes = sim2Limit,
                        isEnabled = sim2Enabled,
                        usedBytes = sim2Used,
                        onOpenSet = { onOpenSetNetworkLimit(NetworkType.MOBILE, 1, sim2Limit) },
                        onDisable = { onDisableNetworkLimit(NetworkType.MOBILE, 1) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNetworkLimitRow(
    networkTitle: String,
    limitBytes: Long?,
    isEnabled: Boolean,
    usedBytes: Long,
    onOpenSet: () -> Unit,
    onDisable: () -> Unit
) {
    val usedFmt = SmartUnitFormatter.format(usedBytes)
    val ratio = if (isEnabled && limitBytes != null && limitBytes > 0L) {
        (usedBytes.toDouble() / limitBytes.toDouble()).coerceIn(0.0, 1.0)
    } else 0.0

    val progressColor = when {
        ratio >= 1.0 -> VlastTokens.SemanticRed
        ratio >= 0.75 -> VlastTokens.SemanticYellow
        else -> VlastTokens.SemanticGreen
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
            .background(VlastTokens.DarkSurface)
            .padding(VlastTokens.Space8)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = networkTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VlastTokens.TextPrimary
                )
                Text(
                    text = if (isEnabled && limitBytes != null && limitBytes > 0L) {
                        val limFmt = SmartUnitFormatter.format(limitBytes)
                        "الحد: ${limFmt.amount} ${limFmt.unit} | المستهلك: ${usedFmt.amount} ${usedFmt.unit}"
                    } else {
                        "بدون حد يومي (مستهلك: ${usedFmt.amount} ${usedFmt.unit})"
                    },
                    fontSize = 10.sp,
                    color = if (isEnabled) progressColor else VlastTokens.TextMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEnabled) {
                    OutlinedButton(
                        onClick = onDisable,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VlastTokens.SemanticRed),
                        shape = RoundedCornerShape(VlastTokens.RadiusSmall),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("إلغاء", fontSize = 10.sp)
                    }
                }
                Button(
                    onClick = onOpenSet,
                    colors = ButtonDefaults.buttonColors(containerColor = VlastTokens.BrandCyan),
                    shape = RoundedCornerShape(VlastTokens.RadiusSmall),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isEnabled) "تعديل" else "تحديد حد",
                        fontSize = 10.sp,
                        color = VlastTokens.DarkBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isEnabled && limitBytes != null && limitBytes > 0L) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { ratio.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = VlastTokens.DarkBorder
            )
        }
    }
}

@Composable
private fun UnsupportedAndroidVersionView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VlastTokens.DarkBackground)
            .padding(VlastTokens.Space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = VlastTokens.BrandAmber,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(VlastTokens.Space16))
        Text(
            text = "الميزة تتطلب إصدار أندرويد أحدث",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = VlastTokens.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(VlastTokens.Space8))
        Text(
            text = "ميزة التحكم الفردي بكل تطبيق تتطلب نظام أندرويد 10 (Android 10 / API 29) أو أحدث، حيث توفر هذه الإصدارات واجهة برمجية للتعرف على التطبيق المالك لكل حزمة بيانات.",
            fontSize = 13.sp,
            color = VlastTokens.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(VlastTokens.Space24))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = VlastTokens.BrandCyan)
        ) {
            Text(
                text = "العودة",
                color = VlastTokens.DarkBackground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
