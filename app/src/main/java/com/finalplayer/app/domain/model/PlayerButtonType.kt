package com.finalplayer.app.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 22 Dynamic Player Controls Buttons defined strictly according to the Icon Guide (دليل الأيقونات).
 */
enum class PlayerButtonType(
    val id: String,
    val title: String
) {
    CHAPTERS("chapters", "الفصول / الإشارات المرجعية"),
    PLAYBACK_SPEED("speed", "سرعة التشغيل"),
    DECODER("decoder", "وحدة فك الترميز"),
    SCREEN_ROTATION("rotate", "تدوير الشاشة"),
    FRAME_BY_FRAME("frame_nav", "التنقل بين الإطارات"),
    VIDEO_ZOOM("zoom", "تكبير الفيديو"),
    PIP_MODE("pip", "صورة داخل صورة"),
    ASPECT_RATIO("aspect_ratio", "نسبة العرض إلى الارتفاع"),
    LOCK_CONTROLS("lock", "قفل عناصر التحكم"),
    AUDIO_TRACK("audio_track", "المسار الصوتي"),
    SUBTITLES("subtitles", "الترجمة"),
    MORE_OPTIONS("more", "المزيد من الخيارات"),
    CURRENT_CHAPTER("current_chapter", "الفصل الحالي"),
    LOOP_MODE("repeat_mode", "وضع التكرار"),
    SHUFFLE("shuffle", "تشغيل عشوائي"),
    FLIP_HORIZONTAL("flip_h", "قلب أفقي"),
    FLIP_VERTICAL("flip_v", "قلب رأسي"),
    AB_REPEAT("ab_repeat", "تكرار A-B"),
    CUSTOM_SKIP("custom_skip", "تخطي مخصص"),
    BACKGROUND_PLAY("background_play", "التشغيل في الخلفية"),
    CINEMA_MODE("cinema", "الوضع السينمائي"),
    SLEEP_TIMER("sleep_timer", "مؤقت النوم");

    val icon: ImageVector
        get() = when (this) {
            CHAPTERS -> Icons.Default.List
            PLAYBACK_SPEED -> Icons.Default.Speed
            DECODER -> Icons.Default.Memory
            SCREEN_ROTATION -> Icons.Default.ScreenRotation
            FRAME_BY_FRAME -> Icons.Default.FastForward
            VIDEO_ZOOM -> Icons.Default.ZoomIn
            PIP_MODE -> Icons.Default.PictureInPicture
            ASPECT_RATIO -> Icons.Default.AspectRatio
            LOCK_CONTROLS -> Icons.Default.Lock
            AUDIO_TRACK -> Icons.Default.Audiotrack
            SUBTITLES -> Icons.Default.Subtitles
            MORE_OPTIONS -> Icons.Default.MoreVert
            CURRENT_CHAPTER -> Icons.Default.BookmarkBorder
            LOOP_MODE -> Icons.Default.Repeat
            SHUFFLE -> Icons.Default.Shuffle
            FLIP_HORIZONTAL -> Icons.Default.FlipToBack
            FLIP_VERTICAL -> Icons.Default.Flip
            AB_REPEAT -> Icons.Default.RepeatOne
            CUSTOM_SKIP -> Icons.Default.Forward10
            BACKGROUND_PLAY -> Icons.Default.Headphones
            CINEMA_MODE -> Icons.Default.Movie
            SLEEP_TIMER -> Icons.Default.Timer
        }

    companion object {
        fun fromId(id: String): PlayerButtonType? {
            return entries.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
        }
    }
}
