package com.finalplayer.app.music.ui.components

import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.finalplayer.app.music.data.db.MusicDatabase
import com.finalplayer.app.music.data.db.PlaylistEntity
import com.finalplayer.app.music.data.db.PlaylistSongCrossRef
import com.finalplayer.app.music.data.model.Song
import com.finalplayer.app.ui.components.DeleteConfirmDialog
import com.finalplayer.app.utils.FileOperationsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    song: Song,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onAddToQueue: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header with song info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    error = rememberVectorPainter(Icons.Default.MusicNote),
                    placeholder = rememberVectorPainter(Icons.Default.MusicNote),
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
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
                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Options List
            SheetOptionItem(
                icon = Icons.Default.PlayArrow,
                title = "تشغيل الآن",
                onClick = {
                    onPlayNow()
                    onDismiss()
                }
            )

            SheetOptionItem(
                icon = Icons.Default.Queue,
                title = "تشغيل بعد الحالية",
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )

            SheetOptionItem(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                title = "إضافة إلى قائمة تشغيل",
                onClick = {
                    showPlaylistSheet = true
                }
            )

            SheetOptionItem(
                icon = Icons.Default.Album,
                title = "عرض الألبوم",
                onClick = {
                    onDismiss()
                    onNavigateToAlbum(song.albumId)
                }
            )

            SheetOptionItem(
                icon = Icons.Default.Person,
                title = "عرض الفنان",
                onClick = {
                    onDismiss()
                    onNavigateToArtist(song.artist)
                }
            )

            SheetOptionItem(
                icon = Icons.Default.Share,
                title = "مشاركة",
                onClick = {
                    onDismiss()
                    FileOperationsUtil.shareSongs(context, listOf(song))
                }
            )

            SheetOptionItem(
                icon = Icons.Default.Info,
                title = "معلومات",
                onClick = {
                    showInfoDialog = true
                }
            )

            SheetOptionItem(
                icon = Icons.Default.Delete,
                title = "حذف",
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    showDeleteConfirmDialog = true
                }
            )
        }
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            itemCount = 1,
            onConfirm = {
                showDeleteConfirmDialog = false
                onDismiss()
                onDelete()
            },
            onDismiss = {
                showDeleteConfirmDialog = false
            }
        )
    }

    if (showInfoDialog) {
        FileInfoDialog(
            song = song,
            onDismiss = {
                showInfoDialog = false
                onDismiss()
            }
        )
    }

    if (showPlaylistSheet) {
        PlaylistPickerSheet(
            songIds = listOf(song.id),
            onDismiss = {
                showPlaylistSheet = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun SheetOptionItem(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}

@Composable
fun FileInfoDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val formattedDuration = remember(song.duration) {
        val totalSeconds = song.duration / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    val format = remember(song.path, song.uri) {
        val pathStr = song.path.ifEmpty { song.uri.toString() }
        val ext = pathStr.substringAfterLast('.', "").uppercase()
        if (ext.isNotEmpty() && ext.length <= 4) ext else "MP3"
    }

    val formattedSize = remember(song.size) {
        if (song.size > 0) Formatter.formatFileSize(context, song.size) else "غير معروف"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("معلومات الملف", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                InfoRow("الاسم:", song.title)
                InfoRow("الفنان:", song.artist)
                InfoRow("الألبوم:", song.album)
                InfoRow("المدة:", formattedDuration)
                InfoRow("الحجم:", formattedSize)
                InfoRow("الصيغة:", format)
                if (song.path.isNotEmpty()) {
                    InfoRow("المسار:", song.path)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("حسناً")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerSheet(
    songIds: List<Long>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { MusicDatabase.getInstance(context) }
    val dao = db.playlistDao()
    val playlists by dao.getAllPlaylists().collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (songIds.size > 1) "إضافة ${songIds.size} أغاني إلى قائمة تشغيل" else "إضافة إلى قائمة تشغيل",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreateDialog = true }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "قائمة تشغيل جديدة",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider()

            LazyColumn {
                items(playlists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch(Dispatchers.IO) {
                                    val refs = songIds.map { sId ->
                                        PlaylistSongCrossRef(
                                            playlistId = playlist.id,
                                            songId = sId
                                        )
                                    }
                                    dao.addSongsToPlaylist(refs)
                                }
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("إنشاء قائمة تشغيل") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("اسم القائمة") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            val name = playlistName.trim()
                            scope.launch(Dispatchers.IO) {
                                val newId = dao.insertPlaylist(PlaylistEntity(name = name))
                                val refs = songIds.map { sId ->
                                    PlaylistSongCrossRef(playlistId = newId, songId = sId)
                                }
                                dao.addSongsToPlaylist(refs)
                            }
                            showCreateDialog = false
                            onDismiss()
                        }
                    }
                ) {
                    Text("إنشاء وإضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerSheet(
    songId: Long,
    onDismiss: () -> Unit
) {
    PlaylistPickerSheet(
        songIds = listOf(songId),
        onDismiss = onDismiss
    )
}

