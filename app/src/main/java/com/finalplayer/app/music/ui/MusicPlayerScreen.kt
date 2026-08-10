package com.finalplayer.app.music.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel = koinViewModel(),
    onBack: () -> Unit,
    onQueueClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val currentLrc by viewModel.currentLrc.collectAsState()
    val currentLineIndex by viewModel.currentLineIndex.collectAsState()

    val context = LocalContext.current
    val primaryThemeColor = MaterialTheme.colorScheme.primary
    val onPrimaryThemeColor = MaterialTheme.colorScheme.onPrimary

    // Controls auto-hide state after 7 seconds of inactivity
    var isControlsVisible by remember { mutableStateOf(true) }
    var interactionTrigger by remember { mutableStateOf(0L) }

    fun userInteracted() {
        if (!isControlsVisible) {
            isControlsVisible = true
        } else {
            interactionTrigger = System.currentTimeMillis()
        }
    }

    LaunchedEffect(isControlsVisible, interactionTrigger) {
        if (isControlsVisible) {
            delay(7000L)
            isControlsVisible = false
        }
    }

    // Two-color palette state extracted from album art
    var primaryBgColor by remember { mutableStateOf(Color(0xFF2C1B3D)) }
    var secondaryBgColor by remember { mutableStateOf(Color(0xFF0F0B18)) }

    val animatedColor1 by animateColorAsState(
        targetValue = primaryBgColor,
        animationSpec = tween(durationMillis = 600),
        label = "BgColor1"
    )
    val animatedColor2 by animateColorAsState(
        targetValue = secondaryBgColor,
        animationSpec = tween(durationMillis = 600),
        label = "BgColor2"
    )

    // Extract 2 palette colors from current song's album art
    LaunchedEffect(state.currentSong?.albumArtUri) {
        val uri = state.currentSong?.albumArtUri
        if (uri == null) {
            primaryBgColor = Color(0xFF2C1B3D)
            secondaryBgColor = Color(0xFF0F0B18)
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        val dom = palette.getDominantColor(0xFF2C1B3D.toInt())
                        val vib = palette.getVibrantColor(
                            palette.getDarkVibrantColor(
                                palette.getMutedColor(0xFF0F0B18.toInt())
                            )
                        )
                        withContext(Dispatchers.Main) {
                            primaryBgColor = Color(dom)
                            secondaryBgColor = Color(vib)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val playButtonScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "PlayButtonScale"
    )

    // Main Full-Screen Layout with Two-Color Album Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedColor1,
                        animatedColor2
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tapping anywhere on screen toggles / restores controls
                isControlsVisible = !isControlsVisible
                if (isControlsVisible) {
                    interactionTrigger = System.currentTimeMillis()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar (Hides after 7s)
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        userInteracted()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.currentSong?.title ?: "كلمات الأغنية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        state.currentSong?.artist?.let { artist ->
                            if (artist.isNotBlank()) {
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    IconButton(onClick = {
                        userInteracted()
                        onQueueClick()
                    }) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "قائمة الانتظار",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Full Screen Unboxed Lyrics (Right-Aligned for Arabic Layout)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val lrc = currentLrc
                if (lrc == null || lrc.lines.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "جارٍ تحميل كلمات الأغنية...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val listState = rememberLazyListState()

                    // Smooth auto-scroll to current line
                    LaunchedEffect(currentLineIndex) {
                        if (currentLineIndex >= 0 && currentLineIndex < lrc.lines.size) {
                            listState.animateScrollToItem(
                                index = (currentLineIndex - 2).coerceAtLeast(0)
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        itemsIndexed(lrc.lines) { index, line ->
                            val isCurrent = index == currentLineIndex
                            val opacity = when {
                                isCurrent -> 1.0f
                                Math.abs(index - currentLineIndex) == 1 -> 0.7f
                                else -> 0.4f
                            }

                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = if (isCurrent) 22.sp else 17.sp,
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isCurrent) primaryThemeColor else Color.White.copy(alpha = opacity)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        userInteracted()
                                        viewModel.seekToLine(line)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Compact Seekbar (Hides after 7s - Uses App Primary Theme Color)
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val positionText = remember(state.positionMs) {
                    formatDuration(state.positionMs)
                }
                val durationText = remember(state.durationMs) {
                    formatDuration(state.durationMs)
                }

                val progress = if (state.durationMs > 0) {
                    (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current Position Time (Left)
                    Text(
                        text = positionText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Thin Line Slider with Circular Thumb matching App Theme Color
                    Slider(
                        value = progress,
                        onValueChange = { percent ->
                            userInteracted()
                            val targetMs = (percent * state.durationMs).toLong()
                            viewModel.seekTo(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = primaryThemeColor,
                            activeTrackColor = primaryThemeColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(3.dp),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = primaryThemeColor,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                thumbTrackGapSize = 0.dp,
                                trackInsideCornerSize = 1.5.dp
                            )
                        },
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(primaryThemeColor, CircleShape)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Total Duration Time (Right)
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Playback Controls Row (Hides after 7s - Uses App Primary Theme Color)
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Repeat Mode Toggle
                    IconButton(onClick = {
                        userInteracted()
                        viewModel.toggleRepeat()
                    }) {
                        val (icon, tint) = when (state.repeatMode) {
                            1 -> Icons.Default.RepeatOne to primaryThemeColor
                            2 -> Icons.Default.Repeat to primaryThemeColor
                            else -> Icons.Default.Repeat to Color.White.copy(alpha = 0.5f)
                        }
                        Icon(imageVector = icon, contentDescription = "تكرار", tint = tint)
                    }

                    // Previous Button
                    IconButton(
                        onClick = {
                            userInteracted()
                            viewModel.skipToPrevious()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "السابق",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Play / Pause Button with App Primary Theme Color
                    FilledIconButton(
                        onClick = {
                            userInteracted()
                            viewModel.togglePlayPause()
                        },
                        modifier = Modifier
                            .size(62.dp)
                            .scale(playButtonScale),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = primaryThemeColor
                        )
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل",
                            modifier = Modifier.size(36.dp),
                            tint = onPrimaryThemeColor
                        )
                    }

                    // Next Button
                    IconButton(
                        onClick = {
                            userInteracted()
                            viewModel.skipToNext()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "التالي",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Shuffle Toggle
                    IconButton(onClick = {
                        userInteracted()
                        viewModel.toggleShuffle()
                    }) {
                        val tint = if (state.shuffleEnabled) {
                            primaryThemeColor
                        } else {
                            Color.White.copy(alpha = 0.5f)
                        }
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "خلط",
                            tint = tint
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
