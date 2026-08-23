package com.finalplayer.app.ui.settings.tabs

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.RadioButton
import com.finalplayer.app.data.preferences.SubtitlesPreferences
import java.io.File
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SubtitlesSettingsTab() {
    val prefs: SubtitlesPreferences = koinInject()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Preference states
    val preferredLangs by prefs.preferredLanguages.asFlow().collectAsState(initial = "eng,en")
    val disableByDefault by prefs.disableByDefault.asFlow().collectAsState(initial = false)
    val autoLoadSubtitles by prefs.autoLoadSubtitles.asFlow().collectAsState(initial = true)
    val overrideAssSubs by prefs.overrideAssSubs.asFlow().collectAsState(initial = false)
    val scaleByWindow by prefs.scaleByWindow.asFlow().collectAsState(initial = true)
    val openInVideoLoc by prefs.openInVideoLocation.asFlow().collectAsState(initial = false)
    val defaultFolder by prefs.defaultFolder.asFlow().collectAsState(initial = "")
    val fontsFolder by prefs.fontsFolder.asFlow().collectAsState(initial = "")

    val saveLocation by prefs.saveLocation.asFlow().collectAsState(initial = "")
    val subtitleSources by prefs.subtitleSources.asFlow().collectAsState(initial = "All,SubDL,Subf2m,OpenSubtitles,Podnapisi,Gestdown,AnimeTosho")
    val searchLanguages by prefs.searchLanguages.asFlow().collectAsState(initial = "English")
    val hearingImpaired by prefs.hearingImpaired.asFlow().collectAsState(initial = false)
    val preferredFormats by prefs.preferredFormats.asFlow().collectAsState(initial = "ASS, SSA, SRT, VTT")
    val preferredEncodings by prefs.preferredEncodings.asFlow().collectAsState(initial = "Unicode (UTF-8)")
    val wyzieApiKey by prefs.wyzieApiKey.asFlow().collectAsState(initial = "")
    val fontSize by prefs.fontSize.asFlow().collectAsState(initial = 21)

    // UI Dialog States
    var showLangsDialog by remember { mutableStateOf(false) }
    var showSourcesDialog by remember { mutableStateOf(false) }
    var showWyzieDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    var showDefaultFolderDialog by remember { mutableStateOf(false) }
    var showFontsFolderDialog by remember { mutableStateOf(false) }
    var showSaveLocationDialog by remember { mutableStateOf(false) }
    var showSearchLangsDialog by remember { mutableStateOf(false) }
    var showFormatsDialog by remember { mutableStateOf(false) }
    var showEncodingsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Section 1 Header: عام
        Text(
            text = "عام",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column {
                // 1. اللغات المفضلة
                SubClickableRow(
                    title = "اللغات المفضلة",
                    subtitle = if (preferredLangs.isBlank()) "غير مُعيَّن (سيستخدم الافتراضي للفيديو)" else preferredLangs,
                    onClick = { showLangsDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 2. تعطيل الترجمات بشكل افتراضي
                SubSwitchRow(
                    title = "تعطيل الترجمات بشكل افتراضي",
                    subtitle = "لن تظهر الترجمات ما لم تُفَعَّل يدوياً أثناء التشغيل.",
                    checked = disableByDefault,
                    onCheckedChange = { newValue ->
                        scope.launch { prefs.disableByDefault.set(newValue) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 3. تحميل الترجمات تلقائياً
                SubSwitchRow(
                    title = "تحميل الترجمات تلقائياً",
                    subtitle = "تحميل الترجمات الخارجية ذات الاسم المطابق تلقائياً.",
                    checked = autoLoadSubtitles,
                    onCheckedChange = { newValue ->
                        scope.launch { prefs.autoLoadSubtitles.set(newValue) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 4. تجاوز تنسيق ترجمات ASS/SSA
                SubSwitchRow(
                    title = "تجاوز تنسيق ترجمات ASS/SSA",
                    subtitle = "فرض تجاوز تنسيق ترجمات ASS/SSA",
                    checked = overrideAssSubs,
                    onCheckedChange = { newValue ->
                        scope.launch { prefs.overrideAssSubs.set(newValue) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 5. تحجيم بحسب النافذة
                SubSwitchRow(
                    title = "تحجيم بحسب النافذة",
                    subtitle = "تحجيم الترجمات بحسب حجم النافذة واستخدام هوامش الفيديو",
                    checked = scaleByWindow,
                    onCheckedChange = { newValue ->
                        scope.launch { prefs.scaleByWindow.set(newValue) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 6. فتح في موقع الفيديو
                SubSwitchRow(
                    title = "فتح في موقع الفيديو",
                    subtitle = "فتح منتقي الملفات في المجلد ذاته للفيديو الحالي",
                    checked = openInVideoLoc,
                    onCheckedChange = { newValue ->
                        scope.launch { prefs.openInVideoLocation.set(newValue) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 7. مجلد التحديد الافتراضي
                SubClickableRow(
                    title = "مجلد التحديد الافتراضي",
                    subtitle = if (defaultFolder.isBlank()) "غير مُعيَّن (سيستخدم الافتراضي للفيديو)" else defaultFolder,
                    onClick = { showDefaultFolderDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 8. مجلد الخطوط
                SubClickableRow(
                    title = "مجلد الخطوط",
                    subtitle = if (fontsFolder.isBlank()) "غير مُعيَّن (يستخدم خطوط النظام)" else fontsFolder,
                    onClick = { showFontsFolderDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2 Header: Subtitle Search / بحث الترجمات
        Text(
            text = "Subtitle Search",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column {
                // 1. موقع حفظ الترجمات
                SubClickableRow(
                    title = "موقع حفظ الترجمات",
                    subtitle = if (saveLocation.isBlank()) "غير مُعيَّن (سيستخدم الافتراضي للفيديو)" else saveLocation,
                    onClick = { showSaveLocationDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 2. Subtitle Sources
                val sourcesDisplay = if (subtitleSources.split(",").size >= 7) "All" else subtitleSources
                SubClickableRow(
                    title = "Subtitle Sources",
                    subtitle = sourcesDisplay,
                    onClick = { showSourcesDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 3. لغات الترجمة
                SubClickableRow(
                    title = "لغات الترجمة",
                    subtitle = searchLanguages,
                    onClick = { showSearchLangsDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 4. Advanced Search Filters Header (Collapsible)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Advanced Search Filters",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Icon(
                        imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(
                    visible = isAdvancedExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                        // Hearing-impaired friendly
                        SubSwitchRow(
                            title = "Hearing-impaired friendly",
                            subtitle = "Only show subtitles optimized for hearing impaired",
                            checked = hearingImpaired,
                            onCheckedChange = { newValue ->
                                scope.launch { prefs.hearingImpaired.set(newValue) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                        // Preferred Formats
                        SubClickableRow(
                            title = "Preferred Formats",
                            subtitle = preferredFormats,
                            onClick = { showFormatsDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                        // Preferred Encodings
                        SubClickableRow(
                            title = "Preferred Encodings",
                            subtitle = preferredEncodings,
                            onClick = { showEncodingsDialog = true }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 5. مسح تنزيلات الترجمة
                SubClickableRow(
                    title = "مسح تنزيلات الترجمة",
                    subtitle = "حذف جميع الملفات في موقع الحفظ الحالي",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearConfirmDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 6. مفتاح Wyzie API
                val apiKeyDisplay = if (wyzieApiKey.isNotBlank()) "•".repeat(wyzieApiKey.length.coerceAtMost(20)) else "...................."
                SubClickableRow(
                    title = "مفتاح Wyzie API",
                    subtitle = apiKeyDisplay,
                    onClick = { showWyzieDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Subtitle Appearance / تنسيق الخط
        Text(
            text = "تنسيق الترجمات",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column {
                SubSliderRow(
                    title = "حجم خط الترجمة",
                    value = fontSize,
                    onValueChange = { newValue ->
                        scope.launch { prefs.fontSize.set(newValue) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Footer Text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "sub.wyzie.io",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Subtitle Search provided by",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Dialogs
    if (showLangsDialog) {
        PreferredLanguagesDialog(
            currentValue = preferredLangs,
            onDismiss = { showLangsDialog = false },
            onConfirm = { newValue ->
                scope.launch { prefs.preferredLanguages.set(newValue) }
                showLangsDialog = false
            }
        )
    }

    if (showSourcesDialog) {
        SubtitleSourcesDialog(
            currentSources = subtitleSources,
            onDismiss = { showSourcesDialog = false },
            onConfirm = { newSources ->
                scope.launch { prefs.subtitleSources.set(newSources) }
                showSourcesDialog = false
            }
        )
    }

    if (showWyzieDialog) {
        WyzieApiKeyDialog(
            currentKey = wyzieApiKey,
            onDismiss = { showWyzieDialog = false },
            onConfirm = { newKey ->
                scope.launch { prefs.wyzieApiKey.set(newKey) }
                showWyzieDialog = false
            }
        )
    }

    if (showDefaultFolderDialog) {
        PathInputDialog(
            title = "مجلد التحديد الافتراضي",
            description = "أدخل مسار مجلد التحديد الافتراضي للترجمات (مثال: /storage/emulated/0/Download/Subtitles)",
            currentValue = defaultFolder,
            onDismiss = { showDefaultFolderDialog = false },
            onConfirm = { newPath ->
                scope.launch { prefs.defaultFolder.set(newPath) }
                showDefaultFolderDialog = false
            }
        )
    }

    if (showFontsFolderDialog) {
        PathInputDialog(
            title = "مجلد الخطوط",
            description = "أدخل مسار مجلد الخطوط المخصصة (أو اتركه فارغاً لاستخدام خطوط النظام)",
            currentValue = fontsFolder,
            onDismiss = { showFontsFolderDialog = false },
            onConfirm = { newPath ->
                scope.launch { prefs.fontsFolder.set(newPath) }
                showFontsFolderDialog = false
            }
        )
    }

    if (showSaveLocationDialog) {
        PathInputDialog(
            title = "موقع حفظ الترجمات",
            description = "أدخل مسار المجلد لحفظ الترجمات المُنَزَّلة (أو اتركه فارغاً لاستخدام مجلد الفيديو)",
            currentValue = saveLocation,
            onDismiss = { showSaveLocationDialog = false },
            onConfirm = { newPath ->
                scope.launch { prefs.saveLocation.set(newPath) }
                showSaveLocationDialog = false
            }
        )
    }

    if (showSearchLangsDialog) {
        MultiSelectOptionDialog(
            title = "لغات الترجمة للبحث",
            allOptions = listOf("Arabic", "English", "French", "Spanish", "German", "Japanese", "Chinese", "Turkish", "Russian", "Italian"),
            currentSelected = searchLanguages,
            onDismiss = { showSearchLangsDialog = false },
            onConfirm = { selected ->
                scope.launch { prefs.searchLanguages.set(selected) }
                showSearchLangsDialog = false
            }
        )
    }

    if (showFormatsDialog) {
        MultiSelectOptionDialog(
            title = "Preferred Formats",
            allOptions = listOf("ASS", "SSA", "SRT", "VTT", "SUB", "MicroDVD"),
            currentSelected = preferredFormats,
            onDismiss = { showFormatsDialog = false },
            onConfirm = { selected ->
                scope.launch { prefs.preferredFormats.set(selected) }
                showFormatsDialog = false
            }
        )
    }

    if (showEncodingsDialog) {
        SingleSelectOptionDialog(
            title = "Preferred Encodings",
            options = listOf("Unicode (UTF-8)", "Arabic (Windows-1256)", "Arabic (ISO-8859-6)", "Western (CP1252)", "Auto-detect"),
            currentSelected = preferredEncodings,
            onDismiss = { showEncodingsDialog = false },
            onConfirm = { selected ->
                scope.launch { prefs.preferredEncodings.set(selected) }
                showEncodingsDialog = false
            }
        )
    }

    if (showClearConfirmDialog) {
        Dialog(onDismissRequest = { showClearConfirmDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "مسح تنزيلات الترجمة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "هل أنت تأكد من أنك تريد حذف جميع ملفات الترجمة المُنَزَّلة؟",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showClearConfirmDialog = false }) {
                            Text("إلغاء", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            showClearConfirmDialog = false
                            var deletedCount = 0
                            val extensions = setOf("srt", "ass", "vtt", "sub", "zip")
                            val targets = mutableListOf<File>()
                            if (saveLocation.isNotBlank()) {
                                val f = File(saveLocation)
                                if (f.exists() && f.isDirectory) targets.add(f)
                            }
                            context.cacheDir?.let { targets.add(it) }
                            context.externalCacheDir?.let { targets.add(it) }

                            targets.forEach { dir ->
                                dir.listFiles()?.forEach { file ->
                                    if (file.isFile && extensions.contains(file.extension.lowercase())) {
                                        if (file.delete()) deletedCount++
                                    }
                                }
                            }
                            Toast.makeText(context, "تم مسح تنزيلات الترجمة بنجاح ($deletedCount ملف)", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("مسح", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubClickableRow(
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SubSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SubSliderRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${sliderValue.toInt()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ThinSlider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it.toInt())
            },
            valueRange = 20f..100f
        )
    }
}

@Composable
private fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedRange<Float> = 20f..100f,
    modifier: Modifier = Modifier
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(valueRange, isRtl) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    if (width > 0) {
                        val fraction = if (isRtl) (1f - offset.x / width) else (offset.x / width)
                        val clampedFraction = fraction.coerceIn(0f, 1f)
                        val rangeSize = valueRange.endInclusive - valueRange.start
                        val newValue = valueRange.start + clampedFraction * rangeSize
                        onValueChange(newValue)
                    }
                }
            }
            .pointerInput(valueRange, isRtl) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val width = size.width.toFloat()
                        if (width > 0) {
                            val rangeSize = valueRange.endInclusive - valueRange.start
                            val deltaFraction = dragAmount / width
                            val adjustedDelta = if (isRtl) -deltaFraction * rangeSize else deltaFraction * rangeSize
                            val newValue = (value + adjustedDelta).coerceIn(valueRange.start, valueRange.endInclusive)
                            onValueChange(newValue)
                        }
                    }
                )
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val rangeSize = valueRange.endInclusive - valueRange.start
        val fraction = if (rangeSize > 0) ((value - valueRange.start) / rangeSize).coerceIn(0f, 1f) else 0f
        val activeWidthPx = widthPx * fraction

        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        val activeColor = MaterialTheme.colorScheme.primary

        // Background Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(trackColor)
        ) {
            // Active Track Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(LocalDensity.current) { activeWidthPx.toDp() })
                    .align(if (isRtl) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(activeColor, CircleShape)
            )
        }

        // Circular Thumb
        val thumbOffsetDp = with(LocalDensity.current) {
            if (isRtl) {
                (widthPx - activeWidthPx - 6.dp.toPx()).toDp()
            } else {
                (activeWidthPx - 6.dp.toPx()).toDp()
            }
        }

        Box(
            modifier = Modifier
                .offset(x = thumbOffsetDp)
                .size(12.dp)
                .align(Alignment.CenterStart)
                .background(activeColor, CircleShape)
        )
    }
}

@Composable
private fun PreferredLanguagesDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "اللغات المفضلة",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "أدخل رموز اللغات مفصولةً بفاصلة (مثال: eng,jpn,spa)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "إلغاء",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = { onConfirm(textValue) }) {
                        Text(
                            text = "حسناً",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleSourcesDialog(
    currentSources: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val allOptions = listOf("SubDL", "Subf2m", "OpenSubtitles", "Podnapisi", "Gestdown", "AnimeTosho")
    val selectedList = remember {
        mutableStateListOf<String>().apply {
            val list = currentSources.split(",").map { it.trim() }
            if (list.contains("All") || list.size >= allOptions.size) {
                addAll(allOptions)
            } else {
                addAll(list.filter { it in allOptions })
            }
        }
    }

    val isAllSelected = selectedList.size == allOptions.size

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Subtitle Sources",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "All" Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isAllSelected) {
                                selectedList.clear()
                            } else {
                                selectedList.clear()
                                selectedList.addAll(allOptions)
                            }
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedList.clear()
                                selectedList.addAll(allOptions)
                            } else {
                                selectedList.clear()
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                allOptions.forEach { option ->
                    val isChecked = selectedList.contains(option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedList.remove(option) else selectedList.add(option)
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedList.add(option) else selectedList.remove(option)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = {
                        val result = if (selectedList.size == allOptions.size) "All" else selectedList.joinToString(",")
                        onConfirm(result)
                    }) {
                        Text(
                            text = "حسناً",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WyzieApiKeyDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(currentKey) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "مفتاح Wyzie API",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "مطلوب للبحث عن الترجمات. احصل على مفتاحك من",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "sub.wyzie.io/redeem",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "إلغاء",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = { onConfirm(textValue) }) {
                        Text(
                            text = "حسناً",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PathInputDialog(
    title: String,
    description: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    singleLine = true,
                    placeholder = { Text("مثال: /storage/emulated/0/Download") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (textValue.isNotBlank()) {
                        TextButton(onClick = { textValue = "" }) {
                            Text("مسح", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = { onConfirm(textValue.trim()) }) {
                        Text("حسناً", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiSelectOptionDialog(
    title: String,
    allOptions: List<String>,
    currentSelected: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val selectedList = remember {
        mutableStateListOf<String>().apply {
            val list = currentSelected.split(",").map { it.trim() }
            addAll(list.filter { it in allOptions })
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                allOptions.forEach { option ->
                    val isChecked = selectedList.contains(option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedList.remove(option) else selectedList.add(option)
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedList.add(option) else selectedList.remove(option)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = { onConfirm(selectedList.joinToString(", ")) }) {
                        Text("حسناً", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleSelectOptionDialog(
    title: String,
    options: List<String>,
    currentSelected: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedOption by remember { mutableStateOf(if (currentSelected in options) currentSelected else options.firstOrNull() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = { selectedOption = option }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = { onConfirm(selectedOption) }) {
                        Text("حسناً", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
