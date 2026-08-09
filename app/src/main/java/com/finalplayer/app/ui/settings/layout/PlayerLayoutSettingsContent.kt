package com.finalplayer.app.ui.settings.layout

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.finalplayer.app.ui.components.AppThinSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.data.preferences.PlayerLayoutPreferences
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun PlayerLayoutSettingsContent(
    onNavigateToEditSection: (String) -> Unit
) {
    val layoutPrefs: PlayerLayoutPreferences = koinInject()
    val scope = rememberCoroutineScope()

    var currentSeekbarStyle by remember { mutableStateOf(layoutPrefs.seekbarStyle.get()) }
    var whiteProgressbar by remember { mutableStateOf(layoutPrefs.whiteProgressbar.get()) }
    var controlsBelowSeekbar by remember { mutableStateOf(layoutPrefs.controlsBelowSeekbar.get()) }
    var elasticAnimations by remember { mutableStateOf(layoutPrefs.elasticAnimations.get()) }
    var hideButtonBg by remember { mutableStateOf(layoutPrefs.hideButtonBackground.get()) }
    var glassmorphismControls by remember { mutableStateOf(layoutPrefs.glassmorphismControls.get()) }
    var glassmorphismSeekbar by remember { mutableStateOf(layoutPrefs.glassmorphismSeekbar.get()) }
    var alwaysDarkMode by remember { mutableStateOf(layoutPrefs.alwaysDarkMode.get()) }
    var showControlsOnStart by remember { mutableStateOf(layoutPrefs.showControlsOnStart.get()) }
    var gradientOpacity by remember { mutableFloatStateOf(layoutPrefs.controlsGradientOpacity.get()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. أدوات التحكم في وضع أفقي (Landscape)
        SectionTitle("أدوات التحكم في وضع أفقي")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column {
                LayoutEditRow(
                    title = "عناصر التحكم العلوية اليمنى",
                    subtitle = "تخصيص الأيقونات والأزرار في أعلى اليمين",
                    onClick = { onNavigateToEditSection("edit_top_right") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                LayoutEditRow(
                    title = "عناصر التحكم السفلي اليمنى",
                    subtitle = "تخصيص أزرار أسفل اليمين بجانب شريط التقدم",
                    onClick = { onNavigateToEditSection("edit_bottom_right") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                LayoutEditRow(
                    title = "عناصر التحكم السفلي اليسرى",
                    subtitle = "تخصيص أزرار أسفل اليسار بجانب الوقت",
                    onClick = { onNavigateToEditSection("edit_bottom_left") }
                )
            }
        }

        // 2. أدوات التحكم في وضع عمودي (Portrait)
        SectionTitle("أدوات التحكم في وضع عمودي")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            LayoutEditRow(
                title = "عناصر التحكم السفلي في الوضع العمودي",
                subtitle = "تخصيص الشريط السفلي للمشغل أثناء الإمساك بالهاتف عمودياً",
                onClick = { onNavigateToEditSection("edit_portrait_bottom") }
            )
        }

        // 3. أدوات التحكم في القائمة الإضافية
        SectionTitle("أدوات التحكم في القائمة الإضافية")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            LayoutEditRow(
                title = "الأزرار في تبويب أدوات التحكم",
                subtitle = "تخصيص ترتيب وإظهار الأزرار داخل ورقة الخيارات الإضافية",
                onClick = { onNavigateToEditSection("edit_controls_tab") }
            )
        }

        // 4. شكل شريط التقدم
        SectionTitle("شكل شريط التقدم")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column {
                val styles = listOf(
                    "standard" to "قياسي",
                    "wavy" to "تموجات (Wavy)",
                    "thick" to "سميك",
                    "circular" to "دائري",
                    "simple" to "بسيط"
                )

                styles.forEachIndexed { idx, (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentSeekbarStyle = key
                                scope.launch { layoutPrefs.seekbarStyle.set(key) }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentSeekbarStyle == key),
                            onClick = {
                                currentSeekbarStyle = key
                                scope.launch { layoutPrefs.seekbarStyle.set(key) }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (currentSeekbarStyle == key) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (idx < styles.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                SwitchSettingRow(
                    title = "شريط تقدم الفيديو باللون الأبيض",
                    subtitle = "استخدام اللون الأبيض لشريط التقدم بدلاً من لون السمة",
                    checked = whiteProgressbar,
                    onCheckedChange = {
                        whiteProgressbar = it
                        scope.launch { layoutPrefs.whiteProgressbar.set(it) }
                    }
                )
            }
        }

        // 5. تخطيط أدوات التحكم
        SectionTitle("تخطيط أدوات التحكم")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            SwitchSettingRow(
                title = "أدوات التحكم أسفل شريط التقدم",
                subtitle = "عرض أزرار التشغيل والخيارات أسفل خط التقدم بدلاً من التراكب",
                checked = controlsBelowSeekbar,
                onCheckedChange = {
                    controlsBelowSeekbar = it
                    scope.launch { layoutPrefs.controlsBelowSeekbar.set(it) }
                }
            )
        }

        // 6. المظهر والتفاعل
        SectionTitle("المظهر والتفاعل")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column {
                SwitchSettingRow(
                    title = "تفعيل الحركات المرنة",
                    subtitle = "تأثيرات ارتدادية وسلسة عند سحب وتفاعل عناصر التحكم",
                    checked = elasticAnimations,
                    onCheckedChange = {
                        elasticAnimations = it
                        scope.launch { layoutPrefs.elasticAnimations.set(it) }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                SwitchSettingRow(
                    title = "إخفاء خلفية أزرار المشغل",
                    subtitle = "جعْل أزرار المشغل شفافة بدون دوائر الخلفية",
                    checked = hideButtonBg,
                    onCheckedChange = {
                        hideButtonBg = it
                        scope.launch { layoutPrefs.hideButtonBackground.set(it) }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                SwitchSettingRow(
                    title = "عناصر تحكم المشغل الزجاجية",
                    subtitle = "تأثير الزجاج المضبب (Glassmorphism) على خلفيات الأزرار",
                    checked = glassmorphismControls,
                    onCheckedChange = {
                        glassmorphismControls = it
                        scope.launch { layoutPrefs.glassmorphismControls.set(it) }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                SwitchSettingRow(
                    title = "خلفية شريط التحريك الزجاجية",
                    subtitle = "إضافة تأثير زجاجي شفاف خلف شريط التقدم السفلي",
                    checked = glassmorphismSeekbar,
                    onCheckedChange = {
                        glassmorphismSeekbar = it
                        scope.launch { layoutPrefs.glassmorphismSeekbar.set(it) }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                SwitchSettingRow(
                    title = "المشغل دائماً في الوضع الداكن",
                    subtitle = "فرض المظهر الأسود القاتم لشاشة المشغل بغض النظر عن سمة التطبيق",
                    checked = alwaysDarkMode,
                    onCheckedChange = {
                        alwaysDarkMode = it
                        scope.launch { layoutPrefs.alwaysDarkMode.set(it) }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                SwitchSettingRow(
                    title = "إظهار أدوات التحكم عند بدء التشغيل",
                    subtitle = "إبقاء عناصر التحكم مرئية تلقائياً عند فتح الفيديو",
                    checked = showControlsOnStart,
                    onCheckedChange = {
                        showControlsOnStart = it
                        scope.launch { layoutPrefs.showControlsOnStart.set(it) }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // Gradient Opacity Slider
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "شفافية التدرج في أدوات التحكم",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تعديل نسبة التعتيم للطبقة خلف عناصر التحكم",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${(gradientOpacity * 100).roundToInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AppThinSlider(
                        value = gradientOpacity,
                        onValueChange = {
                            gradientOpacity = it
                            scope.launch { layoutPrefs.controlsGradientOpacity.set(it) }
                        },
                        valueRange = 0f..1f
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)
    )
}

@Composable
private fun LayoutEditRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "تعديل",
                tint = MaterialTheme.colorScheme.primary
            )
        }
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
