package com.finalplayer.app.ui.settings.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.data.preferences.GesturePreferences
import com.finalplayer.app.data.preferences.PlayerPreferences
import com.finalplayer.app.ui.components.thinScrollbar
import com.finalplayer.app.ui.settings.components.SettingsSectionHeader
import com.finalplayer.app.ui.settings.components.SliderPreferenceItem
import com.finalplayer.app.ui.settings.components.SwitchPreferenceItem
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun GesturesSettingsTab(
    gesturePrefs: GesturePreferences = koinInject(),
    playerPrefs: PlayerPreferences = koinInject()
) {
    val listState = rememberLazyListState()

    // Preferences states
    val brightnessEnabled by gesturePrefs.brightnessGestureEnabled.asFlow().collectAsState(initial = true)
    val volumeEnabled by gesturePrefs.volumeGestureEnabled.asFlow().collectAsState(initial = true)
    val seekEnabled by gesturePrefs.seekGestureEnabled.asFlow().collectAsState(initial = true)
    val pinchZoom by gesturePrefs.pinchToZoom.asFlow().collectAsState(initial = true)
    val subScroll by gesturePrefs.subtitleScrollSeek.asFlow().collectAsState(initial = true)
    val subDrag by gesturePrefs.subtitleDrag.asFlow().collectAsState(initial = true)
    val panZoom by gesturePrefs.panAndZoom.asFlow().collectAsState(initial = false)
    val preventAccidental by gesturePrefs.preventAccidentalSeek.asFlow().collectAsState(initial = false)
    val sensitivity by gesturePrefs.gestureSensitivity.asFlow().collectAsState(initial = 1.0f)
    val swipeSpeed by gesturePrefs.swipeSeekSpeed.asFlow().collectAsState(initial = 1.0f)

    val doubleTapOvals by playerPrefs.showDoubleTapOvals.asFlow().collectAsState(initial = true)
    val showSeekTime by playerPrefs.showSeekTimeWhileSeeking.asFlow().collectAsState(initial = true)
    val swapVolBright by playerPrefs.swapVolumeAndBrightness.asFlow().collectAsState(initial = false)
    val doubleTapDuration by playerPrefs.doubleTapToSeekDuration.asFlow().collectAsState(initial = 10)

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .thinScrollbar(state = listState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        // ═══ Interactive Gesture Demo Test Pad ═══
        item {
            GestureInteractiveDemoCard(
                brightnessEnabled = brightnessEnabled,
                volumeEnabled = volumeEnabled,
                seekEnabled = seekEnabled,
                swapVolBright = swapVolBright,
                doubleTapDuration = doubleTapDuration,
                doubleTapOvals = doubleTapOvals
            )
        }

        // ═══ التحكم بالإيماءات الرئيسية ═══
        item { SettingsSectionHeader("إعدادات الإيماءات الرئيسية") }

        item {
            ListItem(
                headlineContent = { Text("اتجاه الشاشة التلقائي", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("تدوير تلقائي للفيديو حسب اتجاه الهاتف", style = MaterialTheme.typography.bodySmall) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }

        item {
            SwitchPreferenceItem(
                title = "إيماءات السطوع",
                subtitle = "السحب العمودي على جانب الشاشة لضبط السطوع",
                checked = brightnessEnabled,
                onCheckedChange = { gesturePrefs.brightnessGestureEnabled.set(it) }
            )
        }

        item {
            SwitchPreferenceItem(
                title = "إيماءات الصوت",
                subtitle = "السحب العمودي على الجانب المقابل لضبط مستوى الصوت",
                checked = volumeEnabled,
                onCheckedChange = { gesturePrefs.volumeGestureEnabled.set(it) }
            )
        }

        item {
            SwitchPreferenceItem(
                title = "تبديل موضع الصوت والسطوع",
                subtitle = "الصوت جهة اليسار والسطوع جهة اليمين بدلاً من العكس",
                checked = swapVolBright,
                onCheckedChange = { playerPrefs.swapVolumeAndBrightness.set(it) }
            )
        }

        item {
            SwitchPreferenceItem(
                title = "التمرير الأفقي للتقديم والتأخير",
                subtitle = "السحب الأفقي في أي مكان بالشاشة للتقديم في الفيديو",
                checked = seekEnabled,
                onCheckedChange = { gesturePrefs.seekGestureEnabled.set(it) }
            )
        }

        // ═══ الحساسية والشرائط الرفيعة ═══
        item { SettingsSectionHeader("حساسية الإيماءات والسرعة") }

        item {
            SliderPreferenceItem(
                title = "حساسية السحب (السطوع والصوت)",
                subtitle = "مستوى الاستجابة للحركة العمودية: ${(sensitivity * 100).roundToInt()}%",
                value = sensitivity,
                range = 0.5f..2.0f,
                steps = 6,
                onValueChangeFinished = { gesturePrefs.gestureSensitivity.set(it) }
            )
        }

        item {
            SliderPreferenceItem(
                title = "سرعة التقديم السريع بالسحب",
                subtitle = "معدل سرعة تقديم الثواني بالسحب الأفقي: ${(swipeSpeed * 100).roundToInt()}%",
                value = swipeSpeed,
                range = 0.5f..2.5f,
                steps = 8,
                onValueChangeFinished = { gesturePrefs.swipeSeekSpeed.set(it) }
            )
        }

        // ═══ النقر المزدوج ═══
        item { SettingsSectionHeader("إعدادات النقر المزدوج") }

        item {
            DoubleTapDurationSelector(
                currentDuration = doubleTapDuration,
                onDurationSelected = { playerPrefs.doubleTapToSeekDuration.set(it) }
            )
        }

        item {
            SwitchPreferenceItem(
                title = "إظهار موجة النقر المزدوج (Ovals)",
                subtitle = "عرض تأثير تموج بصري عند النقر المزدوج على الجوانب",
                checked = doubleTapOvals,
                onCheckedChange = { playerPrefs.showDoubleTapOvals.set(it) }
            )
        }

        item {
            SwitchPreferenceItem(
                title = "إظهار وقت التقديم",
                subtitle = "عرض الثواني المتقدمة أو المتأخرة أثناء السحب بالإيماءات",
                checked = showSeekTime,
                onCheckedChange = { playerPrefs.showSeekTimeWhileSeeking.set(it) }
            )
        }

        item {
            SwitchPreferenceItem(
                title = "منع التقديم غير المقصود",
                subtitle = "اشتراط مسافة سحب دنيا قبل بدء تقديم الفيديو لمنع اللمس الخطأ",
                checked = preventAccidental,
                onCheckedChange = { gesturePrefs.preventAccidentalSeek.set(it) }
            )
        }

        // ═══ الترجمة والتكبير ═══
        item { SettingsSectionHeader("إيماءات الترجمة والتكبير (Pinch & Zoom)") }

        item {
            SwitchPreferenceItem(
                title = "قرص للتكبير (Pinch to Zoom)",
                subtitle = "استخدام إصبعين لتكبير وتصغير حواف الفيديو",
                checked = pinchZoom,
                onCheckedChange = { gesturePrefs.pinchToZoom.set(it) }
            )
        }

        item {
            SwitchPreferenceItem(
                title = "التحريك مع التكبير (Pan & Zoom)",
                subtitle = "السماح بتحريك إطار الفيديو بحرية بعد تكبيره",
                checked = panZoom,
                onCheckedChange = { gesturePrefs.panAndZoom.set(it) }
            )
        }

        item {
            SwitchPreferenceItem(
                title = "التمرير للتقديم في الترجمة",
                subtitle = "مرّر يميناً أو يساراً عبر شريط الترجمة للانتقال للسطر التالي",
                checked = subScroll,
                onCheckedChange = { gesturePrefs.subtitleScrollSeek.set(it) }
            )
        }

        item {
            SwitchPreferenceItem(
                title = "سحب الترجمة لموضع جديد",
                subtitle = "المس نص الترجمة واسحبه لأعلى أو لأسفل لضبط موقعه البصري",
                checked = subDrag,
                onCheckedChange = { gesturePrefs.subtitleDrag.set(it) }
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun DoubleTapDurationSelector(
    currentDuration: Int,
    onDurationSelected: (Int) -> Unit
) {
    val options = listOf(5, 10, 15, 30)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "مدة التقديم بالنقر المزدوج",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )
        Text(
            text = "اختر عدد الثواني للتقديم أو التأخير عند النقر المزدوج",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { sec ->
                val isSelected = currentDuration == sec
                FilterChip(
                    selected = isSelected,
                    onClick = { onDurationSelected(sec) },
                    label = {
                        Text(
                            text = "$sec ثوانٍ",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun GestureInteractiveDemoCard(
    brightnessEnabled: Boolean,
    volumeEnabled: Boolean,
    seekEnabled: Boolean,
    swapVolBright: Boolean,
    doubleTapDuration: Int,
    doubleTapOvals: Boolean
) {
    var demoFeedbackText by remember { mutableStateOf("اختبر الإيماءات هنا: اسحب للأعلى/الأسفل أو انقر مرتين") }
    var activeDemoIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(Icons.Default.Gesture) }
    var feedbackValue by remember { mutableFloatStateOf(0.5f) }
    var showRippleLeft by remember { mutableStateOf(false) }
    var showRippleRight by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "منطقة تجربة الإيماءات المباشرة",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Demo Pad Box with Touch Listeners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(brightnessEnabled, volumeEnabled, swapVolBright, doubleTapDuration) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val width = size.width
                                if (offset.x < width * 0.5f) {
                                    showRippleLeft = true
                                    demoFeedbackText = "تأخير -$doubleTapDuration ثوانٍ (جهة اليسار)"
                                    activeDemoIcon = Icons.Default.FastRewind
                                } else {
                                    showRippleRight = true
                                    demoFeedbackText = "تقديم +$doubleTapDuration ثوانٍ (جهة اليمين)"
                                    activeDemoIcon = Icons.Default.FastForward
                                }
                            },
                            onTap = {
                                demoFeedbackText = "نقرة واحدة (إظهار / إخفاء عناصر التحكم)"
                                activeDemoIcon = Icons.Default.TouchApp
                            }
                        )
                    }
                    .pointerInput(brightnessEnabled, volumeEnabled, swapVolBright) {
                        var isLeft = false
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isLeft = offset.x < size.width * 0.5f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val isBright = if (swapVolBright) !isLeft else isLeft
                                feedbackValue = (feedbackValue - (dragAmount / 200f)).coerceIn(0f, 1f)
                                if (isBright && brightnessEnabled) {
                                    demoFeedbackText = "السطوع: ${(feedbackValue * 100).roundToInt()}%"
                                    activeDemoIcon = Icons.Default.Brightness6
                                } else if (!isBright && volumeEnabled) {
                                    demoFeedbackText = "الصوت: ${(feedbackValue * 100).roundToInt()}%"
                                    activeDemoIcon = Icons.Default.VolumeUp
                                }
                            }
                        )
                    }
                    .pointerInput(seekEnabled) {
                        if (seekEnabled) {
                            var cumulative = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { cumulative = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    cumulative += dragAmount
                                    val secs = (cumulative / 10f).roundToInt()
                                    demoFeedbackText = if (secs >= 0) "تقديم +${secs}ث" else "تأخير ${secs}ث"
                                    activeDemoIcon = if (secs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Ripple Oval Left
                if (doubleTapOvals && showRippleLeft) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                }

                // Ripple Oval Right
                if (doubleTapOvals && showRippleRight) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    activeDemoIcon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = demoFeedbackText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
