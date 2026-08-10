package com.finalplayer.app.ui.settings.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finalplayer.app.data.preferences.GesturePreferences
import com.finalplayer.app.data.preferences.PlayerPreferences
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Locale

@Composable
fun PlayerSettingsTab(
    prefs: PlayerPreferences = koinInject(),
    gesturePrefs: GesturePreferences = koinInject()
) {
    val scope = rememberCoroutineScope()

    // ═══ State bindings for General ═══
    val orientation by prefs.playerOrientation.asFlow().collectAsState(initial = "video")
    val savePosition by prefs.savePositionOnQuit.asFlow().collectAsState(initial = true)
    val closeAfterPlayback by prefs.closeAfterPlayback.asFlow().collectAsState(initial = true)
    val autoPlayNext by prefs.autoPlayNext.asFlow().collectAsState(initial = true)
    val enablePrevNext by prefs.enablePrevNextButtons.asFlow().collectAsState(initial = true)
    val rememberBrightness by prefs.rememberBrightness.asFlow().collectAsState(initial = false)
    val autoPip by prefs.autoPiPOnNavigation.asFlow().collectAsState(initial = true)
    val keepScreenOnPause by prefs.keepScreenOnPause.asFlow().collectAsState(initial = false)
    val resumeOnUnlock by prefs.resumeOnUnlock.asFlow().collectAsState(initial = false)

    // ═══ State bindings for Seek & Rewind ═══
    val showRipple by prefs.showDoubleTapRipple.asFlow().collectAsState(initial = true)
    val showOvals by prefs.showDoubleTapOvals.asFlow().collectAsState(initial = true)
    val showSeekTime by prefs.showSeekTimeWhileSeeking.asFlow().collectAsState(initial = true)
    val usePreciseSeeking by prefs.usePreciseSeeking.asFlow().collectAsState(initial = false)
    val showSeekbarOnGesture by prefs.showSeekBarOnGesture.asFlow().collectAsState(initial = false)
    val whiteSeekbar by prefs.whiteSeekBar.asFlow().collectAsState(initial = false)
    val hideOsd by prefs.hideOsdText.asFlow().collectAsState(initial = false)
    val customSkipDuration by prefs.customSkipDuration.asFlow().collectAsState(initial = 90)

    // ═══ State bindings for Gestures ═══
    val brightnessGesture by prefs.enableBrightnessGesture.asFlow().collectAsState(initial = true)
    val volumeGesture by prefs.enableVolumeGesture.asFlow().collectAsState(initial = true)
    val pinchToZoom by prefs.enablePinchToZoom.asFlow().collectAsState(initial = true)
    val panAndZoom by prefs.enablePanAndZoom.asFlow().collectAsState(initial = false)
    val horizontalSeek by prefs.enableHorizontalSeek.asFlow().collectAsState(initial = true)
    val subtitleSeekGesture by prefs.enableSubtitleSeekGesture.asFlow().collectAsState(initial = true)
    val subtitleDrag by prefs.enableSubtitleDrag.asFlow().collectAsState(initial = true)
    val seekSensitivity by prefs.seekSensitivity.asFlow().collectAsState(initial = 50)
    val holdSpeed by prefs.holdForMultipleSpeed.asFlow().collectAsState(initial = 2.50f)
    val showDynamicSpeed by prefs.showDynamicSpeed.asFlow().collectAsState(initial = true)

    // ═══ State bindings for Control ═══
    val disableMediaButtons by prefs.disableMediaButtons.asFlow().collectAsState(initial = false)
    val allowPanelGestures by prefs.allowPanelGestures.asFlow().collectAsState(initial = false)
    val swapVolBrightness by prefs.swapVolumeAndBrightness.asFlow().collectAsState(initial = false)
    val showLoadingCircle by prefs.showLoadingCircle.asFlow().collectAsState(initial = true)

    // ═══ State bindings for Screen ═══
    val showStatusBar by prefs.showSystemStatusBar.asFlow().collectAsState(initial = false)
    val showNavBar by prefs.showSystemNavigationBar.asFlow().collectAsState(initial = false)
    val reduceMotion by prefs.reduceMotion.asFlow().collectAsState(initial = true)

    var showOrientationDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        // 1. عام (General)
        item {
            SettingsSectionCard(title = "عام") {
                SettingClickableRow(
                    title = "الاتجاه",
                    subtitle = getOrientationLabel(orientation),
                    onClick = { showOrientationDialog = true }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "حفظ الموضع عند الخروج",
                    checked = savePosition,
                    onCheckedChange = { scope.launch { prefs.savePositionOnQuit.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "الإغلاق بعد انتهاء التشغيل",
                    checked = closeAfterPlayback,
                    onCheckedChange = { scope.launch { prefs.closeAfterPlayback.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "تشغيل الفيديو التالي تلقائياً",
                    subtitle = "تشغيل الفيديو التالي تلقائياً عند انتهاء الحالي",
                    checked = autoPlayNext,
                    onCheckedChange = { scope.launch { prefs.autoPlayNext.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "تفعيل التنقل للأمام/للخلف",
                    subtitle = "إظهار أزرار التالي/السابق لكل فيديوهات المجلد",
                    checked = enablePrevNext,
                    onCheckedChange = { scope.launch { prefs.enablePrevNextButtons.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "تذكر سطوع الشاشة",
                    checked = rememberBrightness,
                    onCheckedChange = { scope.launch { prefs.rememberBrightness.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "صورة داخل صورة تلقائية",
                    subtitle = "الدخول تلقائياً لوضع الصورة داخل الصورة عند الضغط على الرئيسية أو الرجوع",
                    checked = autoPip,
                    onCheckedChange = { scope.launch { prefs.autoPiPOnNavigation.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "إبقاء الشاشة مضاءة عند الإيقاف المؤقت",
                    subtitle = "يمكن للشاشة أن تُطفأ أثناء إيقاف الفيديو مؤقتاً",
                    checked = keepScreenOnPause,
                    onCheckedChange = { scope.launch { prefs.keepScreenOnPause.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "الاستئناف عند إلغاء القفل",
                    subtitle = "إبقاء الفيديو موقوفاً بعد إلغاء قفل الشاشة",
                    checked = resumeOnUnlock,
                    onCheckedChange = { scope.launch { prefs.resumeOnUnlock.set(it) } }
                )
            }
        }

        // 2. التقديم والإرجاع (Seek & Rewind)
        item {
            SettingsSectionCard(title = "التقديم والإرجاع") {
                SettingSwitchRow(
                    title = "إظهار موجة عند التقديم بالنقر المزدوج",
                    checked = showRipple,
                    onCheckedChange = { scope.launch { prefs.showDoubleTapRipple.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "إظهار مؤشر التحريك الدائري عند النقر المزدوج",
                    subtitle = "إظهار طبقة تحريك دائرية مع عرض الوقت أسفلها",
                    checked = showOvals,
                    onCheckedChange = { scope.launch { prefs.showDoubleTapOvals.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "إظهار وقت التقديم",
                    checked = showSeekTime,
                    onCheckedChange = { scope.launch { prefs.showSeekTimeWhileSeeking.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "استخدام التقديم الدقيق",
                    checked = usePreciseSeeking,
                    onCheckedChange = { scope.launch { prefs.usePreciseSeeking.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "إظهار شريط التقديم أثناء التقديم",
                    subtitle = "عرض شريط تقدم الفيديو أثناء التقديم بالإيماءات",
                    checked = showSeekbarOnGesture,
                    onCheckedChange = { scope.launch { prefs.showSeekBarOnGesture.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "شريط تقدم الفيديو باللون الأبيض",
                    subtitle = "عرض شريط تقدم الفيديو باللون الأبيض في شاشة المشغل",
                    checked = whiteSeekbar,
                    onCheckedChange = { scope.launch { prefs.whiteSeekBar.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "إخفاء نص OSD للمشغّل",
                    subtitle = "إخفاء نص OSD الافتراضي لـ mpv أثناء التقديم أو تقديم الترجمة",
                    checked = hideOsd,
                    onCheckedChange = { scope.launch { prefs.hideOsdText.set(it) } }
                )
                ItemDivider()

                var tempSkip by remember(customSkipDuration) { mutableIntStateOf(customSkipDuration) }
                SettingSliderRow(
                    title = "مدة التخطي المخصصة",
                    valueText = "(${tempSkip} s)",
                    subtitle = "المدة عند الضغط على زر التخطي المخصص (${tempSkip} s)",
                    value = tempSkip.toFloat(),
                    valueRange = 5f..180f,
                    onValueChange = { tempSkip = it.toInt() },
                    onValueChangeFinished = { scope.launch { prefs.customSkipDuration.set(tempSkip) } }
                )
            }
        }

        // 3. الإيماءات (Gestures)
        item {
            SettingsSectionCard(title = "الإيماءات") {
                SettingSwitchRow(
                    title = "إيماءات السطوع",
                    checked = brightnessGesture,
                    onCheckedChange = { scope.launch { prefs.enableBrightnessGesture.set(it); gesturePrefs.brightnessGestureEnabled.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "إيماءات الصوت",
                    checked = volumeGesture,
                    onCheckedChange = { scope.launch { prefs.enableVolumeGesture.set(it); gesturePrefs.volumeGestureEnabled.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "قرص للتكبير",
                    checked = pinchToZoom,
                    onCheckedChange = { scope.launch { prefs.enablePinchToZoom.set(it); gesturePrefs.pinchToZoom.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "التحريك والتكبير",
                    subtitle = "السماح بتحريك الفيديو (السحب) إلى جانب التكبير",
                    checked = panAndZoom,
                    onCheckedChange = { scope.launch { prefs.enablePanAndZoom.set(it); gesturePrefs.panAndZoom.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "التمرير الأفقي للتقديم",
                    checked = horizontalSeek,
                    onCheckedChange = { scope.launch { prefs.enableHorizontalSeek.set(it); gesturePrefs.seekGestureEnabled.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "التمرير للتقديم في الترجمة",
                    subtitle = "مرّر يساراً أو يميناً في أعلى أو أسفل الشاشة للتقديم في الترجمة",
                    checked = subtitleSeekGesture,
                    onCheckedChange = { scope.launch { prefs.enableSubtitleSeekGesture.set(it); gesturePrefs.subtitleScrollSeek.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "سحب الترجمة لإعادة تحديد موضعها",
                    subtitle = "المس الترجمة واسحبها لأعلى أو لأسفل لتحريكها. تبقى إيماءات السطوع والصوت فعالة في بقية المناطق",
                    checked = subtitleDrag,
                    onCheckedChange = { scope.launch { prefs.enableSubtitleDrag.set(it); gesturePrefs.subtitleDrag.set(it) } }
                )
                ItemDivider()

                var tempSensitivity by remember(seekSensitivity) { mutableIntStateOf(seekSensitivity) }
                SettingSliderRow(
                    title = "حساسية التمرير الأفقي",
                    valueText = getSensitivityText(tempSensitivity),
                    value = tempSensitivity.toFloat(),
                    valueRange = 10f..100f,
                    onValueChange = { tempSensitivity = it.toInt() },
                    onValueChangeFinished = { scope.launch { prefs.seekSensitivity.set(tempSensitivity) } }
                )
                ItemDivider()

                var tempHoldSpeed by remember(holdSpeed) { mutableFloatStateOf(holdSpeed) }
                SettingSliderRow(
                    title = "الضغط المطوّل لـتشغيل بسرعة مضاعفة",
                    valueText = String.format(Locale.ENGLISH, "%.2fX", tempHoldSpeed),
                    value = tempHoldSpeed,
                    valueRange = 1.25f..4.0f,
                    onValueChange = { tempHoldSpeed = (Math.round(it * 20) / 20f) },
                    onValueChangeFinished = { scope.launch { prefs.holdForMultipleSpeed.set(tempHoldSpeed) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "عرض السرعة الديناميكي",
                    subtitle = "إظهار قيمة السرعة متغيرة أثناء الضغط المطوّل والسحب",
                    checked = showDynamicSpeed,
                    onCheckedChange = { scope.launch { prefs.showDynamicSpeed.set(it) } }
                )
            }
        }

        // 4. التحكم (Control)
        item {
            SettingsSectionCard(title = "التحكم") {
                SettingSwitchRow(
                    title = "تعطيل أزرار الوسائط",
                    subtitle = "تجاهل أوامر التشغيل من سماعات الرأس وأجهزة البلوتوث وغيرها",
                    checked = disableMediaButtons,
                    onCheckedChange = { scope.launch { prefs.disableMediaButtons.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "السماح بالإيماءات في الألواح",
                    checked = allowPanelGestures,
                    onCheckedChange = { scope.launch { prefs.allowPanelGestures.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "تبديل منزلق الصوت والسطوع",
                    checked = swapVolBrightness,
                    onCheckedChange = { scope.launch { prefs.swapVolumeAndBrightness.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "إظهار دائرة التحميل",
                    checked = showLoadingCircle,
                    onCheckedChange = { scope.launch { prefs.showLoadingCircle.set(it) } }
                )
            }
        }

        // 5. الشاشة (Screen)
        item {
            SettingsSectionCard(title = "الشاشة") {
                SettingSwitchRow(
                    title = "إظهار شريط حالة النظام مع عناصر التحكم",
                    checked = showStatusBar,
                    onCheckedChange = { scope.launch { prefs.showSystemStatusBar.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "إظهار شريط التنقل مع عناصر التحكم",
                    checked = showNavBar,
                    onCheckedChange = { scope.launch { prefs.showSystemNavigationBar.set(it) } }
                )
                ItemDivider()
                SettingSwitchRow(
                    title = "تقليل حرَكات المشغِّل",
                    checked = reduceMotion,
                    onCheckedChange = { scope.launch { prefs.reduceMotion.set(it) } }
                )
            }
        }
    }

    if (showOrientationDialog) {
        OrientationSelectionDialog(
            selectedOrientation = orientation,
            onSelect = { newOrientation ->
                scope.launch { prefs.playerOrientation.set(newOrientation) }
            },
            onDismiss = { showOrientationDialog = false }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Reusable UI Components
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            textAlign = TextAlign.End
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content
            )
        }
    }
}

@Composable
fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StyledSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.End
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun SettingSliderRow(
    title: String,
    valueText: String,
    subtitle: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.End
            )
        }

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ThinSlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange
        )
    }
}

@Composable
fun SettingClickableRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun StyledSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedIconColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier.fillMaxWidth(),
        thumb = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(3.dp),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 2.dp
            )
        }
    )
}

@Composable
fun OrientationSelectionDialog(
    selectedOrientation: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "free" to "حر",
        "video" to "الفيديو",
        "smart" to "ذكي",
        "portrait" to "عمودي",
        "portrait_reverse" to "عمودي معكوس",
        "portrait_sensor" to "عمودي بالاستشعار",
        "landscape" to "أفقي",
        "landscape_reverse" to "أفقي معكوس",
        "landscape_sensor" to "أفقي بالاستشعار"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "الاتجاه",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (key == selectedOrientation),
                                onClick = {
                                    onSelect(key)
                                    onDismiss()
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RadioButton(
                            selected = (key == selectedOrientation),
                            onClick = null
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "إلغاء",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

private fun getOrientationLabel(key: String): String {
    return when (key) {
        "free" -> "حر"
        "video" -> "الفيديو"
        "smart" -> "ذكي"
        "portrait" -> "عمودي"
        "portrait_reverse" -> "عمودي معكوس"
        "portrait_sensor" -> "عمودي بالاستشعار"
        "landscape" -> "أفقي"
        "landscape_reverse" -> "أفقي معكوس"
        "landscape_sensor" -> "أفقي بالاستشعار"
        else -> "الفيديو"
    }
}

private fun getSensitivityText(value: Int): String {
    val level = when {
        value < 35 -> "Low"
        value > 65 -> "High"
        else -> "Medium"
    }
    return "Current: $value/100 ($level)"
}
