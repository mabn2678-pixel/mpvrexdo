package com.finalplayer.app.music.ui

import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
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
    var primaryBgColor by remember { mutableStateOf(Color(0xFF3B2820)) }
    var secondaryBgColor by remember { mutableStateOf(Color(0xFF1A1218)) }

    val animatedColor1 by animateColorAsState(
        targetValue = primaryBgColor,
        animationSpec = tween(durationMillis = 800),
        label = "BgColor1"
    )
    val animatedColor2 by animateColorAsState(
        targetValue = secondaryBgColor,
        animationSpec = tween(durationMillis = 800),
        label = "BgColor2"
    )

    // Aurora Motion Animation: Infinite transition moving radial/linear gradients across screen
    val infiniteTransition = rememberInfiniteTransition(label = "AuroraTransition")
    val auroraOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuroraOffset1"
    )
    val auroraOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuroraOffset2"
    )

    var artworkModel by remember { mutableStateOf<Any?>(null) }

    // Resolve artwork model and extract 2 palette colors from current song's album art
    LaunchedEffect(state.currentSong) {
        val song = state.currentSong
        if (song == null) {
            artworkModel = null
            primaryBgColor = Color(0xFF3B2820)
            secondaryBgColor = Color(0xFF1A1218)
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val model: Any? = if (song.albumArtUri != null) {
                song.albumArtUri
            } else {
                getEmbeddedArtwork(context, song.path)
                    ?: song.uri
                    ?: song.path
            }

            withContext(Dispatchers.Main) {
                artworkModel = model
            }

            if (model != null) {
                try {
                    val imageLoader = context.imageLoader
                    val request = ImageRequest.Builder(context)
                        .data(model)
                        .allowHardware(false)
                        .build()
                    val result = imageLoader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            val palette = Palette.from(bitmap).generate()
                            val dom = palette.getDominantColor(0xFF3B2820.toInt())
                            val vib = palette.getVibrantColor(
                                palette.getDarkVibrantColor(
                                    palette.getMutedColor(0xFF1A1218.toInt())
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
    }

    val playButtonScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "PlayButtonScale"
    )

    // Main Container with Animated Aurora Background + Blurred Artist/Album Art Overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isControlsVisible = !isControlsVisible
                if (isControlsVisible) {
                    interactionTrigger = System.currentTimeMillis()
                }
            }
    ) {
        // 1. Layer A: Album Art / Artist Image blurred in background
        if (artworkModel != null) {
            AsyncImage(
                model = artworkModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(28.dp)
                        } else {
                            Modifier
                        }
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                animatedColor1,
                                animatedColor2,
                                Color(0xFF120E15)
                            )
                        )
                    )
            )
        }

        // 2. Layer B: Translucent Animated Two-Color Aurora Mesh Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedColor1.copy(alpha = 0.35f),
                            animatedColor2.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.50f)
                        ),
                        center = Offset(
                            x = 300f + (auroraOffset1 * 500f),
                            y = 400f + (auroraOffset2 * 800f)
                        ),
                        radius = 1200f
                    )
                )
        )

        // Additional Subtle Aurora Shimmer Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            animatedColor2.copy(alpha = 0.20f),
                            animatedColor1.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        start = Offset(x = auroraOffset2 * 600f, y = auroraOffset1 * 400f),
                        end = Offset(x = 800f + auroraOffset1 * 400f, y = 1400f)
                    )
                )
        )

        // 3. Layer C: Dark Atmospheric Vignette Overlay for Crisp White Lyrics Legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )
        )

        // Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header Bar (Matching screenshots: MoreVert options left, Title & Artist center, Down arrow right)
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        userInteracted()
                        onQueueClick()
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات",
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
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "إغلاق",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Full Screen Lyrics Crawling Edge-to-Edge with fixed upper-third active line anchor
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
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
                    val topAnchorPadding = (maxHeight * 0.20f).coerceAtLeast(70.dp)
                    val bottomAnchorPadding = (maxHeight * 0.72f).coerceAtLeast(350.dp)

                    // Smooth auto-scroll to current line anchored in the upper third
                    LaunchedEffect(currentLineIndex) {
                        if (currentLineIndex in lrc.lines.indices) {
                            listState.animateScrollToItem(
                                index = currentLineIndex,
                                scrollOffset = 0
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = topAnchorPadding,
                            bottom = bottomAnchorPadding,
                            start = 8.dp,
                            end = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        itemsIndexed(lrc.lines) { index, line ->
                            val isCurrent = index == currentLineIndex
                            val opacity = when {
                                isCurrent -> 1.0f
                                Math.abs(index - currentLineIndex) == 1 -> 0.60f
                                Math.abs(index - currentLineIndex) == 2 -> 0.35f
                                else -> 0.18f
                            }

                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = if (isCurrent) 26.sp else 20.sp,
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isCurrent) Color.White else Color.White.copy(alpha = opacity),
                                    lineHeight = if (isCurrent) 38.sp else 30.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        userInteracted()
                                        viewModel.seekToLine(line)
                                    }
                                    .padding(vertical = 6.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Bottom Player Controls Area (Progress Bar & Playback Controls matching screenshot)
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
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

                    // Seekbar Slider
                    Slider(
                        value = progress,
                        onValueChange = { percent ->
                            userInteracted()
                            val targetMs = (percent * state.durationMs).toLong()
                            viewModel.seekTo(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(3.dp),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                thumbTrackGapSize = 0.dp,
                                trackInsideCornerSize = 1.5.dp
                            )
                        },
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.White, CircleShape)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )

                    // Time Indicators Below Seekbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = positionText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Play / Pause / Skip Control Buttons (Matching screenshot)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Button
                        IconButton(
                            onClick = {
                                userInteracted()
                                viewModel.skipToPrevious()
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "السابق",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Play / Pause Large White Circle Button
                        FilledIconButton(
                            onClick = {
                                userInteracted()
                                viewModel.togglePlayPause()
                            },
                            modifier = Modifier
                                .size(68.dp)
                                .scale(playButtonScale),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل",
                                modifier = Modifier.size(40.dp),
                                tint = Color.Black
                            )
                        }

                        // Next Button
                        IconButton(
                            onClick = {
                                userInteracted()
                                viewModel.skipToNext()
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "التالي",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
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

private fun getEmbeddedArtwork(context: android.content.Context, pathOrUri: String?): ByteArray? {
    if (pathOrUri.isNullOrBlank()) return null
    return try {
        val retriever = MediaMetadataRetriever()
        if (pathOrUri.startsWith("content://")) {
            retriever.setDataSource(context, Uri.parse(pathOrUri))
        } else {
            retriever.setDataSource(pathOrUri)
        }
        val art = retriever.embeddedPicture
        retriever.release()
        art
    } catch (e: Exception) {
        null
    }
}
