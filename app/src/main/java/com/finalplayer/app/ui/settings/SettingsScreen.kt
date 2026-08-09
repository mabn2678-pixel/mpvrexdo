package com.finalplayer.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.ui.about.AboutScreen
import com.finalplayer.app.ui.settings.tabs.AdvancedSettingsTab
import com.finalplayer.app.ui.settings.tabs.AudioSettingsTab
import com.finalplayer.app.ui.settings.tabs.DecoderSettingsTab
import com.finalplayer.app.ui.settings.tabs.GesturesSettingsTab
import com.finalplayer.app.ui.settings.tabs.PlayerSettingsTab
import com.finalplayer.app.ui.settings.tabs.SubtitlesSettingsTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    initialSubScreen: String? = null
) {
    var currentSubScreen by remember { mutableStateOf<String?>(initialSubScreen) }

    BackHandler(enabled = currentSubScreen != null) {
        if (currentSubScreen?.startsWith("edit") == true) {
            currentSubScreen = "player_layout"
        } else if (currentSubScreen == "libraries") {
            currentSubScreen = "about"
        } else {
            currentSubScreen = null
        }
    }

    when (currentSubScreen) {
        "appearance" -> {
            AppearanceSettingsScreen(onBack = { currentSubScreen = null })
        }
        "player_layout" -> {
            SubScreenContainer(title = "تخطيط المشغِّل", onBack = { currentSubScreen = null }) {
                com.finalplayer.app.ui.settings.layout.PlayerLayoutSettingsContent(
                    onNavigateToEditSection = { currentSubScreen = it }
                )
            }
        }
        "edit_top_right", "edit_bottom_right", "edit_bottom_left", "edit_portrait_bottom", "edit_controls_tab", "edit_overflow_menu" -> {
            com.finalplayer.app.ui.settings.layout.EditLayoutScreen(
                region = currentSubScreen!!,
                onBack = { currentSubScreen = "player_layout" }
            )
        }
        "player" -> {
            SubScreenContainer(title = "المشغِّل", onBack = { currentSubScreen = null }) {
                PlayerSettingsTab()
            }
        }
        "gestures" -> {
            SubScreenContainer(title = "الإيماءات", onBack = { currentSubScreen = null }) {
                GesturesSettingsTab()
            }
        }
        "folders" -> {
            SubScreenContainer(title = "المجلدات", onBack = { currentSubScreen = null }) {
                FoldersSettingsContent()
            }
        }
        "decoder" -> {
            SubScreenContainer(title = "وحدة فك الترميز", onBack = { currentSubScreen = null }) {
                DecoderSettingsTab()
            }
        }
        "subtitles" -> {
            SubScreenContainer(title = "الترجمات", onBack = { currentSubScreen = null }) {
                SubtitlesSettingsTab()
            }
        }
        "audio" -> {
            SubScreenContainer(title = "الصوت", onBack = { currentSubScreen = null }) {
                AudioSettingsTab()
            }
        }
        "rexshorts" -> {
            SubScreenContainer(title = "إعدادات RexShorts", onBack = { currentSubScreen = null }) {
                RexShortsSettingsContent()
            }
        }
        "advanced" -> {
            SubScreenContainer(title = "متقدم", onBack = { currentSubScreen = null }) {
                AdvancedSettingsTab()
            }
        }
        "about" -> {
            AboutScreen(
                onBack = { currentSubScreen = null },
                onOpenLibraries = { currentSubScreen = "libraries" }
            )
        }
        "libraries" -> {
            com.finalplayer.app.ui.about.LibrariesScreen(
                onBack = { currentSubScreen = "about" }
            )
        }
        else -> {
            MainPreferencesScreen(
                onBack = onBack,
                onNavigateToSubScreen = { currentSubScreen = it }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainPreferencesScreen(
    onBack: () -> Unit,
    onNavigateToSubScreen: (String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "التفضيلات",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Category 1: الواجهة والمظهر
            SettingsCategoryHeader(title = "الواجهة والمظهر")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column {
                    SettingsItemRow(
                        title = "المظهر",
                        subtitle = "Material You، الوضع الداكن",
                        icon = Icons.Default.Palette,
                        onClick = { onNavigateToSubScreen("appearance") }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        thickness = 0.8.dp
                    )
                    SettingsItemRow(
                        title = "تخطيط المشغِّل",
                        subtitle = "تخصيص تخطيط أزرار المشغِّل",
                        icon = Icons.Default.Dashboard,
                        onClick = { onNavigateToSubScreen("player_layout") }
                    )
                }
            }

            // Category 2: التشغيل والتحكم
            SettingsCategoryHeader(title = "التشغيل والتحكم")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column {
                    SettingsItemRow(
                        title = "المشغِّل",
                        subtitle = "الاتجاه والإيماءات وعناصر التحكم",
                        icon = Icons.Default.PlayCircle,
                        onClick = { onNavigateToSubScreen("player") }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        thickness = 0.8.dp
                    )
                    SettingsItemRow(
                        title = "الإيماءات",
                        subtitle = "النقر المزدوج، عناصر التحكم في الوسائط",
                        icon = Icons.Default.Gesture,
                        onClick = { onNavigateToSubScreen("gestures") }
                    )
                }
            }

            // Category 3: إدارة الملفات
            SettingsCategoryHeader(title = "إدارة الملفات")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                SettingsItemRow(
                    title = "المجلدات",
                    subtitle = "إدارة قائمة المجلدات المحجوبة",
                    icon = Icons.Default.Folder,
                    onClick = { onNavigateToSubScreen("folders") }
                )
            }

            // Category 4: إعدادات الوسائط
            SettingsCategoryHeader(title = "إعدادات الوسائط")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column {
                    SettingsItemRow(
                        title = "وحدة فك الترميز",
                        subtitle = "فك الترميز بالعتاد، تنسيق البكسل، إزالة التعرج اللوني",
                        icon = Icons.Default.Memory,
                        onClick = { onNavigateToSubScreen("decoder") }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        thickness = 0.8.dp
                    )
                    SettingsItemRow(
                        title = "الترجمات",
                        subtitle = "اللغات المفضلة، الخطوط، والبحث",
                        icon = Icons.Default.Subtitles,
                        onClick = { onNavigateToSubScreen("subtitles") }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        thickness = 0.8.dp
                    )
                    SettingsItemRow(
                        title = "الصوت",
                        subtitle = "اللغات المفضلة، قنوات الصوت، تصحيح طبقة الصوت",
                        icon = Icons.Default.MusicNote,
                        onClick = { onNavigateToSubScreen("audio") }
                    )
                }
            }

            // Category 5: ريكس شورتس
            SettingsCategoryHeader(title = "ريكس شورتس")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                SettingsItemRow(
                    title = "إعدادات RexShorts",
                    subtitle = "إدارة التفعيل والتعطيل والتشغيل العشوائي والمحتوى المحجوب",
                    icon = Icons.Default.VideoLibrary,
                    onClick = { onNavigateToSubScreen("rexshorts") }
                )
            }

            // Category 6: المتقدمة وحول التطبيق
            SettingsCategoryHeader(title = "المتقدمة وحول التطبيق")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column {
                    SettingsItemRow(
                        title = "متقدم",
                        subtitle = "موقع الضبط، mpv.conf",
                        icon = Icons.Default.Code,
                        onClick = { onNavigateToSubScreen("advanced") }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        thickness = 0.8.dp
                    )
                    SettingsItemRow(
                        title = "حول",
                        subtitle = "الشكر والتقدير، التراخيص",
                        icon = Icons.Default.Info,
                        onClick = { onNavigateToSubScreen("about") }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.End
    )
}

@Composable
private fun SettingsItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Titles on the left / end for RTL
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Icon on the right side
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    appearancePrefs: com.finalplayer.app.data.preferences.AppearancePreferences = org.koin.compose.koinInject()
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val themeMode by appearancePrefs.themeMode.asFlow().collectAsState(initial = "system")
    val themePreset by appearancePrefs.themePreset.asFlow().collectAsState(initial = "default")
    val amoledMode by appearancePrefs.amoledMode.asFlow().collectAsState(initial = true)
    val useSystemFont by appearancePrefs.useSystemFont.asFlow().collectAsState(initial = false)
    val matchControlsToTheme by appearancePrefs.matchControlsToTheme.asFlow().collectAsState(initial = false)
    val hideButtonBackgrounds by appearancePrefs.hidePlayerButtonsBackground.asFlow().collectAsState(initial = false)
    val glassmorphismControls by appearancePrefs.glassmorphismControls.asFlow().collectAsState(initial = false)
    val glassmorphismSeekbar by appearancePrefs.glassmorphismSeekbar.asFlow().collectAsState(initial = false)
    val seekbarStyle by appearancePrefs.seekbarStyle.asFlow().collectAsState(initial = "thin")
    val playerAlwaysDark by appearancePrefs.playerAlwaysDark.asFlow().collectAsState(initial = true)

    // Nav Tabs
    val showHomeTab by appearancePrefs.showHomeTab.asFlow().collectAsState(initial = true)
    val showShortsTab by appearancePrefs.showShortsTab.asFlow().collectAsState(initial = true)
    val showRecentsTab by appearancePrefs.showRecentsTab.asFlow().collectAsState(initial = true)

    // File Browser
    val showFullFileNames by appearancePrefs.showFullFileNames.asFlow().collectAsState(initial = false)
    val showNewVideoTag by appearancePrefs.showNewVideoTag.asFlow().collectAsState(initial = true)
    val showAudioFiles by appearancePrefs.showAudioFiles.asFlow().collectAsState(initial = true)
    val showDetailedBreadcrumbs by appearancePrefs.showDetailedBreadcrumbs.asFlow().collectAsState(initial = false)
    val autoScrollToLastVideo by appearancePrefs.autoScrollToLastVideo.asFlow().collectAsState(initial = false)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "المظهر والواجهة",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // "السمة" Header
            Text(
                text = "وضع السمة (الداكن / النهارى)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            // Segmented Theme Mode Row: النظام | فاتح | داكن
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SegmentedButton(
                        text = "النظام",
                        isSelected = themeMode == "system",
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themeMode.set("system") }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedButton(
                        text = "فاتح (نهارى)",
                        isSelected = themeMode == "light",
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themeMode.set("light") }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedButton(
                        text = "داكن",
                        isSelected = themeMode == "dark",
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themeMode.set("dark") }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "سمة التطبيق" App Theme Presets Header
            Text(
                text = "سمة التطبيق (الألوان)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            // Horizontal Scrollable App Theme Cards
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ThemePresetCard(
                        title = "افتراضي (mpv)",
                        primaryColor = Color(0xFF00E676),
                        secondaryColor = Color(0xFF00C853),
                        dotColor = Color(0xFF1B5E20),
                        isSelected = themePreset.equals("default", ignoreCase = true),
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themePreset.set("default") }
                        },
                        modifier = Modifier.width(110.dp)
                    )
                }
                item {
                    ThemePresetCard(
                        title = "Lavender",
                        primaryColor = Color(0xFFD0BCFF),
                        secondaryColor = Color(0xFFCCC2DC),
                        dotColor = Color(0xFFE8DEF8),
                        isSelected = themePreset.equals("Lavender", ignoreCase = true),
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themePreset.set("Lavender") }
                        },
                        modifier = Modifier.width(110.dp)
                    )
                }
                item {
                    ThemePresetCard(
                        title = "Kanagawa",
                        primaryColor = Color(0xFF98BB6C),
                        secondaryColor = Color(0xFF7E9CD8),
                        dotColor = Color(0xFFE6C384),
                        isSelected = themePreset.equals("Kanagawa", ignoreCase = true),
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themePreset.set("Kanagawa") }
                        },
                        modifier = Modifier.width(110.dp)
                    )
                }
                item {
                    ThemePresetCard(
                        title = "Gruvbox",
                        primaryColor = Color(0xFF83A598),
                        secondaryColor = Color(0xFFFE8019),
                        dotColor = Color(0xFFD5C4A1),
                        isSelected = themePreset.equals("Gruvbox", ignoreCase = true),
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themePreset.set("Gruvbox") }
                        },
                        modifier = Modifier.width(110.dp)
                    )
                }
                item {
                    ThemePresetCard(
                        title = "Catppuccin",
                        primaryColor = Color(0xFFCBA6F7),
                        secondaryColor = Color(0xFFF5E0DC),
                        dotColor = Color(0xFF89B4FA),
                        isSelected = themePreset.equals("Catppuccin", ignoreCase = true),
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themePreset.set("Catppuccin") }
                        },
                        modifier = Modifier.width(110.dp)
                    )
                }
                item {
                    ThemePresetCard(
                        title = "Cloudflare",
                        primaryColor = Color(0xFFF48120),
                        secondaryColor = Color(0xFFFAAE42),
                        dotColor = Color(0xFFD96B00),
                        isSelected = themePreset.equals("Cloudflare", ignoreCase = true),
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themePreset.set("Cloudflare") }
                        },
                        modifier = Modifier.width(110.dp)
                    )
                }
                item {
                    ThemePresetCard(
                        title = "Doom",
                        primaryColor = Color(0xFFFF5555),
                        secondaryColor = Color(0xFFFF79C6),
                        dotColor = Color(0xFFBD93F9),
                        isSelected = themePreset.equals("Doom", ignoreCase = true),
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themePreset.set("Doom") }
                        },
                        modifier = Modifier.width(110.dp)
                    )
                }
                item {
                    ThemePresetCard(
                        title = "Cotton Candy",
                        primaryColor = Color(0xFFFF79C6),
                        secondaryColor = Color(0xFF8BE9FD),
                        dotColor = Color(0xFFBD93F9),
                        isSelected = themePreset.equals("Cotton_Candy", ignoreCase = true),
                        onClick = {
                            coroutineScope.launch { appearancePrefs.themePreset.set("Cotton_Candy") }
                        },
                        modifier = Modifier.width(110.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // General Appearance Switches Group
            Text(
                text = "تخصيص الأزرار والخطوط والأنماط",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column {
                    SwitchSettingRow(
                        title = "وضع AMOLED الأسود",
                        subtitle = "استخدام خلفية سوداء خالصة للسمات الداكنة",
                        checked = amoledMode,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.amoledMode.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "استخدام خط النظام",
                        subtitle = "استخدام الخط الافتراضي للجهاز بدلاً من خط التطبيق",
                        checked = useSystemFont,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.useSystemFont.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "تصميم الشرائط الرفيعة لشريط التقدم",
                        subtitle = "استخدام شريط تقدم رفيع لمظهر عصري وأنيق",
                        checked = seekbarStyle == "thin",
                        onCheckedChange = { isThin ->
                            coroutineScope.launch { appearancePrefs.seekbarStyle.set(if (isThin) "thin" else "default") }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "عناصر تحكّم المشغِّل الزجاجية",
                        subtitle = "تفعيل تأثير الزجاج الضبابي (Glassmorphism)",
                        checked = glassmorphismControls,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.glassmorphismControls.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "خلفية شريط التحريك الزجاجية",
                        subtitle = "تفعيل المظهر الزجاجي لشريط التقدم",
                        checked = glassmorphismSeekbar,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.glassmorphismSeekbar.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "مطابقة عناصر التحكم للسمة",
                        subtitle = "تلوين أزرار المشغِّل بألوان السمة الحالية",
                        checked = matchControlsToTheme,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.matchControlsToTheme.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "إخفاء خلفية أزرار المشغِّل",
                        subtitle = "إخفاء خلفية جميع أزرار التحكم في المشغِّل",
                        checked = hideButtonBackgrounds,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.hidePlayerButtonsBackground.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "المشغِّل دائماً في الوضع الداكن",
                        subtitle = "الحفاظ على واجهة المشغل داكنة حتى لو كان التطبيق في الوضع النهارى",
                        checked = playerAlwaysDark,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.playerAlwaysDark.set(newValue) }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Tabs Category
            Text(
                text = "تبويبات شريط التنقل السفلي",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column {
                    SwitchSettingRow(
                        title = "عرض تبويب الشاشة الرئيسية",
                        subtitle = "إظهار المجلدات والملفات في الشريط السفلي",
                        checked = showHomeTab,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.showHomeTab.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "عرض تبويب RexShorts",
                        subtitle = "إظهار تبويب الفيديوهات القصيرة",
                        checked = showShortsTab,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.showShortsTab.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "عرض تبويب المشغّل مؤخراً",
                        subtitle = "إظهار قائمة السجل والسجل المباشر",
                        checked = showRecentsTab,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.showRecentsTab.set(newValue) }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // File Browser Options Category
            Text(
                text = "مستعرض الملفات والعرض",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column {
                    SwitchSettingRow(
                        title = "عرض الأسماء الكاملة للملفات",
                        subtitle = "عدم قطع أو اختصار أسماء ملفات الفيديو الطويلة",
                        checked = showFullFileNames,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.showFullFileNames.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "إظهار شارة (جديد) للفيديوهات الحديثة",
                        subtitle = "تمييز الفيديوهات المضافة حديثاً بشارة ملونة",
                        checked = showNewVideoTag,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.showNewVideoTag.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "إظهار الملفات الصوتية في المستعرض",
                        subtitle = "عرض ملفات MP3 و FLAC و WAV جنباً إلى جنب مع الفيديوهات",
                        checked = showAudioFiles,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.showAudioFiles.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "إظهار مسار التنقل التفصيلي",
                        subtitle = "عرض شريط Breadcrumbs أعلى المجلدات",
                        checked = showDetailedBreadcrumbs,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.showDetailedBreadcrumbs.set(newValue) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), thickness = 0.8.dp)
                    SwitchSettingRow(
                        title = "التمرير التلقائي لآخر فيديو تم تشغيله",
                        subtitle = "الانتقال التلقائي لموقع آخر فيديو عند فتح المجلد",
                        checked = autoScrollToLastVideo,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch { appearancePrefs.autoScrollToLastVideo.set(newValue) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        modifier = modifier.height(44.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ThemePresetCard(
    title: String,
    primaryColor: Color,
    secondaryColor: Color,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        // Phone mockup preview
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top bar mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // Middle pill mockups
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(primaryColor)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(secondaryColor)
                    )
                }

                // Bottom bar mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Switch on Left / Start
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            thumbContent = if (!checked) {
                {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            } else null
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Titles on Right / End
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubScreenContainer(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            content()
        }
    }
}

@Composable
private fun FoldersSettingsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "المجلدات المحجوبة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "لا توجد مجلدات محجوبة حالياً. يمكنك حجب أي مجلد من شاشة المجلدات الرئيسية.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubtitlesSettingsContent() {
    var autoLoadSubtitles by remember { mutableStateOf(true) }
    var subtitleEncoding by remember { mutableStateOf("UTF-8") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column {
                SwitchSettingRow(
                    title = "تحميل الترجمات تلقائياً",
                    subtitle = "تحميل ملفات الترجمة ذات الاسم المماثل للفيديو تلقائياً",
                    checked = autoLoadSubtitles,
                    onCheckedChange = { autoLoadSubtitles = it }
                )
            }
        }
    }
}

@Composable
private fun RexShortsSettingsContent() {
    var enableRexShorts by remember { mutableStateOf(true) }
    var autoPlayNext by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column {
                SwitchSettingRow(
                    title = "تفعيل ميزة RexShorts",
                    subtitle = "تفعيل تشغيل الفيديوهات القصيرة",
                    checked = enableRexShorts,
                    onCheckedChange = { enableRexShorts = it }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                SwitchSettingRow(
                    title = "التشغيل التلقائي للتالي",
                    subtitle = "الانتقال إلى الفيديو القصير التالي تلقائياً عند انتهاء الحالي",
                    checked = autoPlayNext,
                    onCheckedChange = { autoPlayNext = it }
                )
            }
        }
    }
}
