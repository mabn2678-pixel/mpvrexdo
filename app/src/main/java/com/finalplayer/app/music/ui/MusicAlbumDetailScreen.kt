package com.finalplayer.app.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.finalplayer.app.music.data.model.Song
import com.finalplayer.app.music.ui.components.MusicMiniPlayer
import com.finalplayer.app.music.ui.components.SongOptionsSheet
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicAlbumDetailScreen(
    albumId: Long,
    viewModel: MusicLibraryViewModel = koinViewModel(),
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val allSongs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val playerState by viewModel.controller.state.collectAsState()

    val album = remember(albums, albumId) {
        albums.find { it.id == albumId }
    }

    val albumSongs = remember(allSongs, albumId) {
        allSongs.filter { it.albumId == albumId }
    }

    var selectedSongForSheet by remember { mutableStateOf<Song?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val albumTitle = album?.title ?: albumSongs.firstOrNull()?.album ?: "الألبوم"
    val artistName = album?.artist ?: albumSongs.firstOrNull()?.artist ?: "فنان غير معروف"
    val albumArtUri = album?.albumArtUri ?: albumSongs.firstOrNull()?.albumArtUri

    val totalDurationMs = remember(albumSongs) {
        albumSongs.sumOf { it.duration }
    }

    val yearText = if ((album?.year ?: 0) > 0) "${album?.year} · " else ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = albumTitle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        bottomBar = {
            MusicMiniPlayer(
                state = playerState,
                onPlayPauseClick = { viewModel.controller.togglePlayPause() },
                onNextClick = { viewModel.controller.skipToNext() },
                onClick = onOpenPlayer
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header item
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        error = rememberVectorPainter(Icons.Default.MusicNote),
                        placeholder = rememberVectorPainter(Icons.Default.MusicNote),
                        modifier = Modifier
                            .size(200.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = albumTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$yearText${albumSongs.size} أغنية · ${formatTotalDuration(totalDurationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (albumSongs.isNotEmpty()) {
                                    viewModel.playSong(albumSongs.first(), albumSongs)
                                    onOpenPlayer()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("▶ تشغيل الكل")
                        }

                        OutlinedButton(
                            onClick = {
                                if (albumSongs.isNotEmpty()) {
                                    val shuffled = albumSongs.shuffled()
                                    viewModel.playSong(shuffled.first(), shuffled)
                                    onOpenPlayer()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⇄ خلط")
                        }
                    }
                }
            }

            // Songs list items: track# | title | duration | ⋮
            itemsIndexed(albumSongs, key = { _, song -> song.id }) { index, song ->
                AlbumSongRow(
                    trackNumber = index + 1,
                    song = song,
                    onClick = {
                        viewModel.playSong(song, albumSongs)
                        onOpenPlayer()
                    },
                    onOptionsClick = {
                        selectedSongForSheet = song
                    }
                )
            }
        }

        selectedSongForSheet?.let { song ->
            SongOptionsSheet(
                song = song,
                sheetState = sheetState,
                onDismiss = { selectedSongForSheet = null },
                onPlayNow = {
                    viewModel.playSong(song, albumSongs)
                    onOpenPlayer()
                },
                onAddToQueue = { viewModel.controller.addToQueue(song) },
                onNavigateToAlbum = { },
                onNavigateToArtist = { }
            )
        }
    }
}

@Composable
private fun AlbumSongRow(
    trackNumber: Int,
    song: Song,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val durationText = remember(song.duration) {
        val totalSeconds = song.duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$trackNumber",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = durationText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(onClick = onOptionsClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "خيارات",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTotalDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
