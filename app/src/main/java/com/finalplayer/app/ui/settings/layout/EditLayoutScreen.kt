package com.finalplayer.app.ui.settings.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.data.preferences.LayoutPreferences
import com.finalplayer.app.data.preferences.PlayerLayoutPreferences
import com.finalplayer.app.domain.model.PlayerButtonType
import com.finalplayer.app.ui.components.AppFlowRow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditLayoutScreen(
    region: String,
    onBack: () -> Unit = {}
) {
    val layoutPrefs: LayoutPreferences = koinInject()
    val legacyPrefs: PlayerLayoutPreferences = koinInject()
    val scope = rememberCoroutineScope()

    val (title, pref, legacyPref, defaultVal) = when (region.lowercase()) {
        "top_right_controls", "top_right", "edit_top_right" -> Quadruple(
            "تعديل المنطقة العلوية اليمنى",
            layoutPrefs.topRightControls,
            legacyPrefs.topRightControls,
            LayoutPreferences.DEFAULT_TOP_RIGHT
        )
        "bottom_right_controls", "bottom_right", "edit_bottom_right" -> Quadruple(
            "تعديل المنطقة السفلى اليمنى",
            layoutPrefs.bottomRightControls,
            legacyPrefs.bottomRightControls,
            LayoutPreferences.DEFAULT_BOTTOM_RIGHT
        )
        "bottom_left_controls", "bottom_left", "edit_bottom_left" -> Quadruple(
            "تعديل المنطقة السفلى اليسرى",
            layoutPrefs.bottomLeftControls,
            legacyPrefs.bottomLeftControls,
            LayoutPreferences.DEFAULT_BOTTOM_LEFT
        )
        "overflow_menu_controls", "overflow_menu", "controls_tab", "edit_controls_tab", "edit_overflow_menu" -> Quadruple(
            "تعديل قائمة الخيارات الإضافية",
            layoutPrefs.overflowMenuControls,
            legacyPrefs.controlsTabButtons,
            LayoutPreferences.DEFAULT_OVERFLOW_MENU
        )
        "portrait_bottom_controls", "portrait_bottom", "edit_portrait_bottom" -> Quadruple(
            "تعديل عناصر التحكم السفلى (عمودي)",
            layoutPrefs.portraitBottomControls,
            legacyPrefs.portraitBottomControls,
            LayoutPreferences.DEFAULT_PORTRAIT_BOTTOM
        )
        else -> Quadruple(
            "تعديل تخطيط المشغل",
            layoutPrefs.topRightControls,
            legacyPrefs.topRightControls,
            LayoutPreferences.DEFAULT_TOP_RIGHT
        )
    }

    val currentRawPref by pref.asFlow().collectAsState(initial = defaultVal)
    var activeButtons by remember(currentRawPref) {
        mutableStateOf(layoutPrefs.parseButtonTypeList(currentRawPref))
    }
    var showResetDialog by remember { mutableStateOf(false) }

    fun saveButtons(newList: List<PlayerButtonType>) {
        activeButtons = newList
        val formatted = layoutPrefs.formatButtonTypeList(newList)
        scope.launch {
            pref.set(formatted)
            legacyPref.set(formatted)
        }
    }

    val availableButtons = remember(activeButtons) {
        PlayerButtonType.entries.filter { button -> !activeButtons.contains(button) }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("إعادة ضبط التخطيط", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد أنك تريد إعادة ضبط هذه المنطقة إلى الإعدادات الافتراضية؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val defaultList = layoutPrefs.parseButtonTypeList(defaultVal)
                        saveButtons(defaultList)
                        showResetDialog = false
                    }
                ) {
                    Text("إعادة ضبط", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "إعادة ضبط الافتراضي"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Helper info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "قم بتخصيص الأزرار الظاهرة في هذه المنطقة. يمكنك إضافة أو إزالة أي من الأزرار الـ 22 المتاحة.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                }
            }

            // Section 1: Active buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "الأزرار المفعلة (${activeButtons.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (activeButtons.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "لا توجد أزرار مفعلة في هذه المنطقة حالياً",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    AppFlowRow(
                        horizontalSpacing = 8.dp,
                        verticalSpacing = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        activeButtons.forEach { button ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = button.icon,
                                        contentDescription = button.title,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = button.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                            .clickable {
                                                saveButtons(activeButtons.filter { it != button })
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "إزالة",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Section 2: Available buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "الأزرار المتاحة للإضافة (${availableButtons.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (availableButtons.isEmpty()) {
                    Text(
                        text = "تم تفعيل جميع الأزرار الـ 22 في هذه المنطقة!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AppFlowRow(
                        horizontalSpacing = 8.dp,
                        verticalSpacing = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableButtons.forEach { button ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.clickable {
                                    saveButtons(activeButtons + button)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = button.icon,
                                        contentDescription = button.title,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = button.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "إضافة",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Section 3: All 22 Buttons Toggle List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "دليل الأيقونات المعتمد (22 زراً)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                PlayerButtonType.entries.forEach { button ->
                    val isEnabled = activeButtons.contains(button)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isEnabled) {
                                    saveButtons(activeButtons.filter { it != button })
                                } else {
                                    saveButtons(activeButtons + button)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = button.icon,
                                            contentDescription = button.title,
                                            tint = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = button.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }

                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!activeButtons.contains(button)) {
                                            saveButtons(activeButtons + button)
                                        }
                                    } else {
                                        saveButtons(activeButtons.filter { it != button })
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
