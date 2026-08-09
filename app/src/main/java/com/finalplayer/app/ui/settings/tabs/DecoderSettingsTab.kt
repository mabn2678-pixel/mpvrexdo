package com.finalplayer.app.ui.settings.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.data.preferences.DecoderPreferences
import com.finalplayer.app.player.core.MPVLib
import com.finalplayer.app.ui.components.thinScrollbar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun DecoderSettingsTab() {
    val prefs: DecoderPreferences = koinInject()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val tryHW by prefs.tryHWDecoding.asFlow().collectAsState(initial = prefs.tryHWDecoding.get())
    val gpuNext by prefs.gpuNext.asFlow().collectAsState(initial = prefs.gpuNext.get())
    val useVulkan by prefs.useVulkan.asFlow().collectAsState(initial = prefs.useVulkan.get())
    val profile by prefs.profile.asFlow().collectAsState(initial = prefs.profile.get())
    val debanding by prefs.debanding.asFlow().collectAsState(initial = prefs.debanding.get())
    val useYUV420P by prefs.useYUV420P.asFlow().collectAsState(initial = prefs.useYUV420P.get())
    val anime4k by prefs.anime4k.asFlow().collectAsState(initial = prefs.anime4k.get())
    val hdrToSdr by prefs.hdrToSdr.asFlow().collectAsState(initial = prefs.hdrToSdr.get())

    var showProfileDialog by remember { mutableStateOf(false) }
    var showDebandDialog by remember { mutableStateOf(false) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .thinScrollbar(state = listState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                text = "وحدة فك الترميز",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 4.dp)
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. MPV Profile
                    SettingClickableItem(
                        title = "ملف تعريف MPV",
                        subtitle = profile,
                        onClick = { showProfileDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 2. HW Decoding
                    SettingSwitchItem(
                        title = "محاولة فك الترميز بالعتاد",
                        subtitle = null,
                        checked = tryHW,
                        onCheckedChange = { newValue ->
                            scope.launch {
                                prefs.tryHWDecoding.set(newValue)
                                try {
                                    MPVLib.setPropertyString("hwdec", if (newValue) "mediacodec,mediacodec-copy,no" else "no")
                                } catch (_: Throwable) {}
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 3. gpu-next
                    SettingSwitchItem(
                        title = "استخدام gpu-next",
                        subtitle = "خلفية عرض جديدة",
                        checked = gpuNext,
                        onCheckedChange = { newValue ->
                            scope.launch {
                                prefs.gpuNext.set(newValue)
                                try {
                                    MPVLib.setPropertyString("vo", if (newValue) "gpu-next" else "gpu")
                                } catch (_: Throwable) {}
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 4. Vulkan
                    SettingSwitchItem(
                        title = "استخدام Vulkan (Experimental)",
                        subtitle = "غير مدعوم (يتطلب Android 13+ مع Vulkan 1.3)",
                        isSubtitleWarning = true,
                        checked = useVulkan,
                        onCheckedChange = { newValue ->
                            scope.launch {
                                prefs.useVulkan.set(newValue)
                                try {
                                    MPVLib.setPropertyString("gpu-api", if (newValue) "vulkan" else "opengl")
                                } catch (_: Throwable) {}
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 5. Debanding
                    SettingClickableItem(
                        title = "إزالة التعرج اللوني",
                        subtitle = debanding,
                        onClick = { showDebandDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 6. YUV420P
                    SettingSwitchItem(
                        title = "استخدام تنسيق البكسل YUV420P",
                        subtitle = "قد يصلح الشاشات السوداء على بعض مُرمّزات الفيديو، وقد يحسن الأداء على حساب الجودة",
                        checked = useYUV420P,
                        onCheckedChange = { newValue ->
                            scope.launch {
                                prefs.useYUV420P.set(newValue)
                                try {
                                    MPVLib.setOptionString("vf", if (newValue) "format=yuv420p" else "")
                                } catch (_: Throwable) {}
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 7. Anime4K
                    SettingSwitchItem(
                        title = "Anime4K upscaling (Experimental)",
                        subtitle = "Enable Anime4K upscaling filter\ngithub.com/bloc97/Anime4K",
                        checked = anime4k,
                        onCheckedChange = { newValue ->
                            scope.launch {
                                prefs.anime4k.set(newValue)
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 8. HDR-to-SDR
                    SettingSwitchItem(
                        title = "HDR-to-SDR Tone Mapping (hdr-toys)",
                        subtitle = "Apply high quality GLSL shaders for HDR-to-SDR conversion. Requires gpu-next\ngithub.com/natural-harmonia-gropius/hdr-toys",
                        checked = hdrToSdr,
                        onCheckedChange = { newValue ->
                            scope.launch {
                                prefs.hdrToSdr.set(newValue)
                            }
                        }
                    )
                }
            }
        }
    }

    // --- Profile Dialog ---
    if (showProfileDialog) {
        val profileOptions = listOf("Fast", "Default", "High Quality", "GPU HQ", "Low Latency", "SW Fast")
        RadioButtonSelectionDialog(
            title = "ملف تعريف MPV",
            options = profileOptions,
            selectedOption = profile,
            onDismiss = { showProfileDialog = false },
            onSelect = { selected ->
                scope.launch {
                    prefs.profile.set(selected)
                    val mpvVal = when(selected.lowercase()) {
                        "fast" -> "fast"
                        "default" -> "default"
                        "high quality" -> "high-quality"
                        "gpu hq" -> "gpu-hq"
                        "low latency" -> "low-latency"
                        "sw fast" -> "sw-fast"
                        else -> "fast"
                    }
                    try {
                        MPVLib.setPropertyString("profile", mpvVal)
                    } catch (_: Throwable) {}
                }
                showProfileDialog = false
            }
        )
    }

    // --- Debanding Dialog ---
    if (showDebandDialog) {
        val debandOptions = listOf("None", "CPU", "GPU")
        RadioButtonSelectionDialog(
            title = "إزالة التعرج اللوني",
            options = debandOptions,
            selectedOption = debanding,
            onDismiss = { showDebandDialog = false },
            onSelect = { selected ->
                scope.launch {
                    prefs.debanding.set(selected)
                    try {
                        when (selected) {
                            "CPU" -> MPVLib.command("vf", "add", "@deband:gradfun=radius=12")
                            "GPU" -> MPVLib.setOptionString("deband", "yes")
                            else -> {
                                MPVLib.command("vf", "remove", "@deband")
                                MPVLib.setOptionString("deband", "no")
                            }
                        }
                    } catch (_: Throwable) {}
                }
                showDebandDialog = false
            }
        )
    }
}

@Composable
private fun SettingClickableItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!subtitle.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String?,
    isSubtitleWarning: Boolean = false,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = if (isSubtitleWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun RadioButtonSelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                options.forEach { option ->
                    val isSelected = option.equals(selectedOption, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(option) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
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
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
