package com.finalplayer.app.ui.player.controls.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.data.preferences.PlayerLayoutPreferences
import com.finalplayer.app.ui.components.SidePanel
import com.finalplayer.app.ui.player.Decoder
import com.finalplayer.app.ui.player.Sheets
import org.koin.compose.koinInject
import java.util.Locale

@Composable
fun MoreSheet(
    sleepTimerRemaining: Int = 0,
    currentSpeed: Float = 1.0f,
    currentDecoder: Decoder = Decoder.HW_PLUS,
    currentAspectRatio: String = "default",
    currentZoom: Float = 1.0f,
    repeatMode: Int = 0,
    isShuffle: Boolean = false,
    isCinemaMode: Boolean = false,
    isBackgroundPlay: Boolean = false,
    onOpenSheet: (Sheets) -> Unit,
    onDismiss: () -> Unit,
    onToggleRotate: () -> Unit = {},
    onToggleLock: () -> Unit = {},
    onEnterPiP: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onFrameStep: (Boolean) -> Unit = {},
    onFlipVideo: (Boolean) -> Unit = {},
    onToggleAbRepeat: () -> Unit = {},
    onCustomSkip: () -> Unit = {},
    onToggleCinema: () -> Unit = {},
    onToggleBackgroundPlay: () -> Unit = {},
    layoutPrefs: PlayerLayoutPreferences = koinInject()
) {
    SidePanel(onDismissRequest = onDismiss, scrollable = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "MPV",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Text(
                        text = "خيارات المشغل",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Category 1: Media & Tracks
            SectionHeader("الوسائط والمسارات")

            MoreDrawerItem(
                icon = Icons.Default.Subtitles,
                title = "الترجمة",
                badgeText = "مسارات / إعدادات",
                onClick = {
                    onDismiss()
                    onOpenSheet(Sheets.SubtitleTracks)
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.Audiotrack,
                title = "المسار الصوتي",
                badgeText = "اختيار الصوت",
                onClick = {
                    onDismiss()
                    onOpenSheet(Sheets.AudioTracks)
                }
            )

            MoreDrawerItem(
                icon = Icons.AutoMirrored.Filled.List,
                title = "الفصول / الإشارات",
                badgeText = "استعراض",
                onClick = {
                    onDismiss()
                    onOpenSheet(Sheets.Chapters)
                }
            )

            MoreDrawerItem(
                icon = Icons.AutoMirrored.Filled.List,
                title = "قائمة التشغيل",
                badgeText = "القائمة",
                onClick = {
                    onDismiss()
                    onOpenSheet(Sheets.Playlist)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Category 2: Playback & Performance
            SectionHeader("سرعة وجودة التشغيل")

            MoreDrawerItem(
                icon = Icons.Default.Speed,
                title = "سرعة التشغيل",
                badgeText = String.format(Locale.US, "%.2fx", currentSpeed),
                highlightBadge = kotlin.math.abs(currentSpeed - 1.0f) > 0.01f,
                onClick = {
                    onDismiss()
                    onOpenSheet(Sheets.PlaybackSpeed)
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.Memory,
                title = "وحدة فك الترميز",
                badgeText = currentDecoder.displayName,
                onClick = {
                    onDismiss()
                    onOpenSheet(Sheets.Decoders)
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.AspectRatio,
                title = "نسبة العرض",
                badgeText = when (currentAspectRatio) {
                    "16:9" -> "16:9"
                    "4:3" -> "4:3"
                    "21:9" -> "21:9"
                    "fill" -> "تعبئة"
                    else -> "تلقائي"
                },
                onClick = {
                    onDismiss()
                    onOpenSheet(Sheets.AspectRatios)
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.ZoomIn,
                title = "تكبير الفيديو",
                badgeText = "${(currentZoom * 100).toInt()}%",
                onClick = {
                    onDismiss()
                    onOpenSheet(Sheets.VideoZoom)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Category 3: Screen & Controls
            SectionHeader("أدوات الشاشة والتحكم")

            MoreDrawerItem(
                icon = Icons.Default.Timer,
                title = "مؤقت النوم",
                badgeText = if (sleepTimerRemaining > 0) {
                    String.format(Locale.US, "%d:%02d", sleepTimerRemaining / 60, sleepTimerRemaining % 60)
                } else {
                    "معطل"
                },
                highlightBadge = sleepTimerRemaining > 0,
                onClick = {
                    onDismiss()
                    onOpenSheet(Sheets.SleepTimer)
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.PictureInPicture,
                title = "صورة داخل صورة (PiP)",
                onClick = {
                    onDismiss()
                    onEnterPiP()
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.ScreenRotation,
                title = "تدوير الشاشة",
                onClick = {
                    onDismiss()
                    onToggleRotate()
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.Lock,
                title = "قفل عناصر التحكم",
                onClick = {
                    onDismiss()
                    onToggleLock()
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.FastForward,
                title = "التنقل بين الإطارات",
                onClick = {
                    onDismiss()
                    onOpenSheet(Sheets.FrameNav)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Category 4: Modes & Switches
            SectionHeader("أوضاع التشغيل")

            MoreSwitchDrawerItem(
                icon = Icons.Default.Movie,
                title = "الوضع السينمائي",
                checked = isCinemaMode,
                onCheckedChange = {
                    onToggleCinema()
                }
            )

            MoreSwitchDrawerItem(
                icon = Icons.Default.Headphones,
                title = "التشغيل في الخلفية",
                checked = isBackgroundPlay,
                onCheckedChange = {
                    onToggleBackgroundPlay()
                }
            )

            MoreDrawerItem(
                icon = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                title = "وضع التكرار",
                badgeText = when (repeatMode) {
                    1 -> "تكرار الملف"
                    2 -> "تكرار الكل"
                    else -> "إيقاف التكرار"
                },
                highlightBadge = repeatMode > 0,
                onClick = {
                    onToggleRepeat()
                }
            )

            MoreSwitchDrawerItem(
                icon = Icons.Default.Shuffle,
                title = "التشغيل العشوائي",
                checked = isShuffle,
                onCheckedChange = {
                    onToggleShuffle()
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.RepeatOne,
                title = "تكرار A-B",
                onClick = {
                    onDismiss()
                    onToggleAbRepeat()
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.Flip,
                title = "قلب رأسي",
                onClick = {
                    onDismiss()
                    onFlipVideo(true)
                }
            )

            MoreDrawerItem(
                icon = Icons.Default.FlipToBack,
                title = "قلب أفقي",
                onClick = {
                    onDismiss()
                    onFlipVideo(false)
                }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        ),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun MoreDrawerItem(
    icon: ImageVector,
    title: String,
    badgeText: String? = null,
    highlightBadge: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!badgeText.isNullOrEmpty()) {
                Surface(
                    color = if (highlightBadge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (highlightBadge) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreSwitchDrawerItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(width = 36.dp, height = 24.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}
