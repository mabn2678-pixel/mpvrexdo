package com.finalplayer.app.ui.settings.tabs

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.finalplayer.app.data.preferences.AudioPreferences
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AudioSettingsTab() {
    val prefs: AudioPreferences = koinInject()
    val scope = rememberCoroutineScope()

    val preferredLangs by prefs.preferredLanguages.asFlow().collectAsState(initial = "")
    val pitchCorrection by prefs.audioPitchCorrection.asFlow().collectAsState(initial = true)
    val normalization by prefs.volumeNormalization.asFlow().collectAsState(initial = false)
    val backgroundPlay by prefs.backgroundPlayEnabled.asFlow().collectAsState(initial = false)
    val channels by prefs.audioChannels.asFlow().collectAsState(initial = "auto-safe")
    val volumeBoostCap by prefs.volumeBoostCap.asFlow().collectAsState(initial = 30)

    var showLangsDialog by remember { mutableStateOf(false) }
    var showChannelsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Section Title: الصوت
        Text(
            text = "الصوت",
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
                AudioClickableRow(
                    title = "اللغات المفضلة",
                    subtitle = if (preferredLangs.isBlank()) "غير مُعيَّن (سيستخدم الافتراضي للفيديو)" else preferredLangs,
                    onClick = { showLangsDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 2. تفعيل تصحيح طبقة الصوت
                AudioSwitchRow(
                    title = "تفعيل تصحيح طبقة الصوت",
                    subtitle = "يمنع ارتفاع طبقة الصوت عند السرعات الأعلى وانخفاضها عند السرعات الأبطأ",
                    checked = pitchCorrection,
                    onCheckedChange = { newValue ->
                        scope.launch { prefs.audioPitchCorrection.set(newValue) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 3. توحيد مستوى الصوت
                AudioSwitchRow(
                    title = "توحيد مستوى الصوت",
                    subtitle = "ضبط مستوى الصوت تلقائياً للحفاظ على مستوى ثابت",
                    checked = normalization,
                    onCheckedChange = { newValue ->
                        scope.launch { prefs.volumeNormalization.set(newValue) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 4. التشغيل في الخلفية
                AudioSwitchRow(
                    title = "التشغيل في الخلفية",
                    subtitle = "التشغيل في الخلفية",
                    checked = backgroundPlay,
                    onCheckedChange = { newValue ->
                        scope.launch { prefs.backgroundPlayEnabled.set(newValue) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 5. قنوات الصوت
                val channelLabel = when (channels) {
                    "auto" -> "تلقائي"
                    "auto-safe" -> "تلقائي آمن"
                    "mono" -> "أحادي"
                    "stereo" -> "ستيريو"
                    "reverse-stereo" -> "ستيريو معكوس"
                    else -> "تلقائي آمن"
                }
                AudioClickableRow(
                    title = "قنوات الصوت",
                    subtitle = channelLabel,
                    onClick = { showChannelsDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // 6. حد رفع مستوى الصوت
                AudioSliderRow(
                    title = "حد رفع مستوى الصوت",
                    value = volumeBoostCap,
                    onValueChange = { newValue ->
                        scope.launch { prefs.volumeBoostCap.set(newValue) }
                    }
                )
            }
        }
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

    if (showChannelsDialog) {
        AudioChannelsDialog(
            currentValue = channels,
            onDismiss = { showChannelsDialog = false },
            onSelect = { selectedChannel ->
                scope.launch { prefs.audioChannels.set(selectedChannel) }
                showChannelsDialog = false
            }
        )
    }
}

@Composable
private fun AudioClickableRow(
    title: String,
    subtitle: String,
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
}

@Composable
private fun AudioSwitchRow(
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
private fun AudioSliderRow(
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

        ThinVolumeSlider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it.toInt())
            },
            valueRange = 0f..100f
        )
    }
}

@Composable
private fun ThinVolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedRange<Float> = 0f..100f,
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
                .height(4.dp)
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
                    text = "لغة (لغات) الصوت التي تُحدَّد افتراضياً على الفيديو ذي المسارات الصوتية المتعددة. تُقبل رموز اللغات المكوّنة من حرفين أو ثلاثة. يمكن الفصل بين قيم متعددة بفاصلة.",
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
private fun AudioChannelsDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "auto" to "تلقائي",
        "auto-safe" to "تلقائي آمن",
        "mono" to "أحادي",
        "stereo" to "ستيريو",
        "reverse-stereo" to "ستيريو معكوس"
    )

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
                    text = "قنوات الصوت",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(key) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            fontWeight = if (currentValue == key) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        RadioButton(
                            selected = currentValue == key,
                            onClick = { onSelect(key) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "إلغاء",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
