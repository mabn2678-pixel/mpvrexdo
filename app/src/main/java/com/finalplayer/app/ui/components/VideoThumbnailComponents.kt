package com.finalplayer.app.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.videoFrameMillis

private var globalVideoImageLoader: ImageLoader? = null

fun getVideoImageLoader(context: Context): ImageLoader {
    return globalVideoImageLoader ?: synchronized(VideoThumbnailImageProviderLock) {
        globalVideoImageLoader ?: ImageLoader.Builder(context.applicationContext)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(context.applicationContext)
                    .maxSizePercent(0.30)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.applicationContext.cacheDir.resolve("video_thumbnails_cache"))
                    .maxSizeBytes(150 * 1024 * 1024)
                    .build()
            }
            .crossfade(false)
            .build()
            .also { globalVideoImageLoader = it }
    }
}

private object VideoThumbnailImageProviderLock

@Composable
fun rememberVideoImageLoader(context: Context = LocalContext.current): ImageLoader {
    val appContext = context.applicationContext
    return remember(appContext) {
        getVideoImageLoader(appContext)
    }
}

@Composable
fun VideoThumbnailImage(
    videoUri: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailUrl: String? = null,
    videoDurationMs: Long = 0L
) {
    val context = LocalContext.current
    val imageLoader = rememberVideoImageLoader(context)
    val placeholderPainter = rememberVectorPainter(Icons.Default.PlayCircleOutline)

    val modelToLoad = thumbnailUrl?.ifBlank { null } ?: videoUri.ifBlank { null }

    // Seek 3 seconds (or 1/4 duration) to avoid black intro frames
    val targetFrameMs = remember(videoDurationMs) {
        if (videoDurationMs > 0) {
            minOf(videoDurationMs / 4, 3000L).coerceAtLeast(1000L)
        } else {
            3000L
        }
    }

    val request = remember(modelToLoad, context, targetFrameMs) {
        ImageRequest.Builder(context)
            .data(modelToLoad)
            .videoFrameMillis(targetFrameMs)
            .crossfade(false)
            .build()
    }

    AsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        placeholder = placeholderPainter,
        error = placeholderPainter
    )
}

@Composable
fun VideoStatusBadge(
    isOpened: Boolean,
    modifier: Modifier = Modifier
) {
    if (isOpened) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shadowElevation = 2.dp
        ) {
            Text(
                text = "Running",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 2.dp
        ) {
            Text(
                text = "New",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

