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
    onOpenSetAppLimit: (packageName: String, appName: String, currentLimitBytes: Long?) -> Unit,
    onDisableAppLimit: (packageName: String, appName: String) -> Unit,
    onUpdateAppNetworkTargets: (packageName: String, appName: String, targetWifi: Boolean, targetSim1: Boolean, targetSim2: Boolean) -> Unit = { _, _, _, _, _ -> },
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
            val launcherActivities = pm.queryIntentActivities(intent, 0)
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
                            onOpenSetLimit = { onOpenSetAppLimit(app.packageName, app.appName, rule?.dailyLimitBytes) },
                            onDisableLimit = { onDisableAppLimit(app.packageName, app.appName) },
                            onUpdateNetworkTargets = { wifi, sim1, sim2 ->
                                onUpdateAppNetworkTargets(app.packageName, app.appName, wifi, sim1, sim2)
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
                        onOpenSetLimit = { onOpenSetAppLimit(app.packageName, app.appName, rule?.dailyLimitBytes) },
                        onDisableLimit = { onDisableAppLimit(app.packageName, app.appName) },
                        onUpdateNetworkTargets = { wifi, sim1, sim2 ->
                            onUpdateAppNetworkTargets(app.packageName, app.appName, wifi, sim1, sim2)
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
    onOpenSetLimit: () -> Unit,
    onDisableLimit: () -> Unit,
    onUpdateNetworkTargets: (targetWifi: Boolean, targetSim1: Boolean, targetSim2: Boolean) -> Unit
) {
    val isBlocked = rule?.isFullyBlocked == true
    val hasLimit = rule?.dailyLimitEnabled == true && (rule.dailyLimitBytes ?: 0L) > 0L
    val usedBytes = rule?.usedBytesToday ?: 0L
    val limitBytes = rule?.dailyLimitBytes ?: 0L
    val targetWifi = rule?.targetWifi ?: true
    val targetSim1 = rule?.targetSim1 ?: true
    val targetSim2 = rule?.targetSim2 ?: true

    val ratio = if (hasLimit && limitBytes > 0L) {
        (usedBytes.toDouble() / limitBytes.toDouble()).coerceIn(0.0, 1.0)
    } else 0.0

    val progressColor = when {
        ratio >= 1.0 -> VlastTokens.SemanticRed
        ratio >= 0.75 -> VlastTokens.SemanticYellow
        else -> VlastTokens.SemanticGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VlastTokens.RadiusMedium))
            .background(VlastTokens.DarkSurface)
            .border(
                width = 1.dp,
                color = when {
                    isBlocked -> VlastTokens.SemanticRed.copy(alpha = 0.5f)
                    hasLimit && rule?.isLimitReached == true -> VlastTokens.SemanticRed.copy(alpha = 0.5f)
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

                    // Badges or Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isBlocked) {
                            Text(
                                text = "محظور بالكامل (${rule?.networkTargetSummary ?: "جميع الشبكات"})",
                                fontSize = 11.sp,
                                color = VlastTokens.SemanticRed,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (hasLimit) {
                            val limitDisplay = SmartUnitFormatter.format(limitBytes)
                            Text(
                                text = "حد يومي: ${limitDisplay.amount} ${limitDisplay.unit} (${rule?.networkTargetSummary ?: "جميع الشبكات"})",
                                fontSize = 11.sp,
                                color = progressColor,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            val usedDisplay = SmartUnitFormatter.format(usedBytes)
                            Text(
                                text = "استهلاك اليوم: ${usedDisplay.amount} ${usedDisplay.unit}",
                                fontSize = 11.sp,
                                color = VlastTokens.TextMuted
                            )
                        }
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
                } else if (hasLimit && rule?.isLimitReached == true) {
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

                    // 2) Daily Limit Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الحد اليومي المخصص",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = VlastTokens.TextPrimary
                            )
                            Text(
                                text = if (hasLimit) {
                                    val lim = SmartUnitFormatter.format(limitBytes)
                                    "الحد الحالي: ${lim.amount} ${lim.unit}"
                                } else {
                                    "لا يوجد حد يومي مفعل (استهلاك غير محدود)"
                                },
                                fontSize = 11.sp,
                                color = VlastTokens.TextMuted
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (hasLimit) {
                                OutlinedButton(
                                    onClick = onDisableLimit,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VlastTokens.SemanticRed),
                                    shape = RoundedCornerShape(VlastTokens.RadiusSmall)
                                ) {
                                    Text("إلغاء الحد", fontSize = 11.sp)
                                }
                            }
                            Button(
                                onClick = onOpenSetLimit,
                                colors = ButtonDefaults.buttonColors(containerColor = VlastTokens.BrandCyan),
                                shape = RoundedCornerShape(VlastTokens.RadiusSmall)
                            ) {
                                Text(
                                    text = if (hasLimit) "تعديل الحد" else "تحديد حد",
                                    fontSize = 11.sp,
                                    color = VlastTokens.DarkBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 3) Network Selection Section
                    Spacer(modifier = Modifier.height(VlastTokens.Space12))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                            .background(VlastTokens.DarkSurface)
                            .padding(VlastTokens.Space8)
                    ) {
                        Text(
                            text = "نطاق الشبكة (الشرائح والواي فاي المطبقة):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VlastTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Wifi Choice Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                                    .background(if (targetWifi) VlastTokens.BrandCyan else VlastTokens.DarkSurfaceVariant)
                                    .border(1.dp, if (targetWifi) VlastTokens.BrandCyan else VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusSmall))
                                    .clickable {
                                        onUpdateNetworkTargets(!targetWifi, targetSim1, targetSim2)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "الواي فاي ${if (targetWifi) "✓" else ""}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetWifi) VlastTokens.DarkBackground else VlastTokens.TextMuted
                                )
                            }

                            // SIM 1 Choice Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                                    .background(if (targetSim1) VlastTokens.BrandCyan else VlastTokens.DarkSurfaceVariant)
                                    .border(1.dp, if (targetSim1) VlastTokens.BrandCyan else VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusSmall))
                                    .clickable {
                                        onUpdateNetworkTargets(targetWifi, !targetSim1, targetSim2)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "الشريحة 1 ${if (targetSim1) "✓" else ""}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetSim1) VlastTokens.DarkBackground else VlastTokens.TextMuted
                                )
                            }

                            // SIM 2 Choice Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                                    .background(if (targetSim2) VlastTokens.BrandCyan else VlastTokens.DarkSurfaceVariant)
                                    .border(1.dp, if (targetSim2) VlastTokens.BrandCyan else VlastTokens.DarkBorder, RoundedCornerShape(VlastTokens.RadiusSmall))
                                    .clickable {
                                        onUpdateNetworkTargets(targetWifi, targetSim1, !targetSim2)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "الشريحة 2 ${if (targetSim2) "✓" else ""}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetSim2) VlastTokens.DarkBackground else VlastTokens.TextMuted
                                )
                            }
                        }
                    }

                    // 4) Usage Display with Semantic Color
                    Spacer(modifier = Modifier.height(VlastTokens.Space12))
                    val usedFormatted = SmartUnitFormatter.format(usedBytes)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VlastTokens.RadiusSmall))
                            .background(VlastTokens.DarkSurface)
                            .padding(VlastTokens.Space8)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "استهلاك التطبيق اليوم:",
                                fontSize = 11.sp,
                                color = VlastTokens.TextSecondary
                            )
                            Text(
                                text = "${usedFormatted.amount} ${usedFormatted.unit}",
                                fontSize = 12.sp,
                                fontFamily = NumberFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            )
                        }

                        if (hasLimit && limitBytes > 0L) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { ratio.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = progressColor,
                                trackColor = VlastTokens.DarkBorder
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val remaining = (limitBytes - usedBytes).coerceAtLeast(0L)
                                val remainingFormatted = SmartUnitFormatter.format(remaining)
                                Text(
                                    text = if (rule?.isLimitReached == true) "تم استهلاك الحد المخصص بالكامل" else "المتبقي: ${remainingFormatted.amount} ${remainingFormatted.unit}",
                                    fontSize = 10.sp,
                                    color = if (rule?.isLimitReached == true) VlastTokens.SemanticRed else VlastTokens.TextMuted
                                )
                                val pct = (ratio * 100).toInt()
                                Text(
                                    text = "$pct%",
                                    fontSize = 10.sp,
                                    fontFamily = NumberFontFamily,
                                    color = progressColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
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
