package com.finalplayer.app.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.finalplayer.app.music.data.model.Album
import com.finalplayer.app.music.data.model.Song
import com.finalplayer.app.music.ui.components.MusicMiniPlayer
import com.finalplayer.app.music.ui.components.SongOptionsSheet
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicArtistDetailScreen(
    artistName: String,
    viewModel: MusicLibraryViewModel = koinViewModel(),
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onOpenPlayer: () -> Unit
) {
    val allSongs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val playerState by viewModel.controller.state.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var showSortSheet by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("title") } // "title", "date", "duration"
    var sortAscending by remember { mutableStateOf(true) }
    var isGridView by remember { mutableStateOf(false) }

    // Match all songs belonging to this artist (including featured/collaboration tracks)
    val artistSongs = remember(allSongs, artistName) {
        val target = artistName.trim().lowercase()
        val splitRegex = Regex("[,&;/]|\\bfeat\\.?\\b|\\bft\\.?\\b|\\band\\b", RegexOption.IGNORE_CASE)

        allSongs.filter { song ->
            val songArtist = song.artist.trim().lowercase()
            if (songArtist == target || songArtist.contains(target)) {
                true
            } else {
                val tokens = songArtist.split(splitRegex).map { it.trim() }
                tokens.any { it == target || (it.length > 2 && target.length > 2 && (it.contains(target) || target.contains(it))) }
            }
        }
    }

    val artistAlbums = remember(albums, artistSongs, artistName) {
        val songAlbumIds = artistSongs.map { it.albumId }.toSet()
        albums.filter { album ->
            songAlbumIds.contains(album.id) || album.artist.contains(artistName, ignoreCase = true)
        }
    }

    // Filter ONLY the opened artist's songs and albums when searching inside this artist screen
    val filteredSongs = remember(artistSongs, searchQuery, sortBy, sortAscending) {
        val searched = if (searchQuery.isBlank()) {
            artistSongs
        } else {
            artistSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.album.contains(searchQuery, ignoreCase = true)
            }
        }

        when (sortBy) {
            "title" -> if (sortAscending) searched.sortedBy { it.title } else searched.sortedByDescending { it.title }
            "date" -> if (sortAscending) searched.sortedBy { it.dateAdded } else searched.sortedByDescending { it.dateAdded }
            "duration" -> if (sortAscending) searched.sortedBy { it.duration } else searched.sortedByDescending { it.duration }
            else -> searched
        }
    }

    val filteredAlbums = remember(artistAlbums, searchQuery) {
        if (searchQuery.isBlank()) artistAlbums else artistAlbums.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }
    }

    val artistArtUri = remember(artistSongs) {
        artistSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri
    }

    var selectedSongForSheet by remember { mutableStateOf<Song?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val initials = remember(artistName) {
        artistName.trim().take(1).uppercase()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث في أغانٍ وألبومات $artistName...") },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = artistName,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            isSearchActive = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "بحث"
                        )
                    }

                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "خيارات الفرز والعرض"
                        )
                    }
                }
            )
        },
        bottomBar = {
            MusicMiniPlayer(
                state = playerState,
                onPlayPauseClick = { viewModel.controller.togglePlayPause() },
                onPreviousClick = { viewModel.controller.skipToPrevious() },
                onNextClick = { viewModel.controller.skipToNext() },
                onCloseClick = { viewModel.controller.stop() },
                onClick = onOpenPlayer
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header: Artist Image or Initials Avatar + Name + Stats
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (artistArtUri != null) {
                        AsyncImage(
                            model = artistArtUri,
                            contentDescription = artistName,
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Default.Person),
                            placeholder = rememberVectorPainter(Icons.Default.Person),
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${artistAlbums.size} ألبوم · ${artistSongs.size} أغنية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Section "الألبومات"
            if (filteredAlbums.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "الألبومات (${filteredAlbums.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredAlbums, key = { it.id }) { album ->
                                ArtistAlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Section "الأغاني"
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الأغاني (${filteredSongs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "تبديل وضع العرض"
                        )
                    }
                }
            }

            if (filteredSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة داخل $artistName" else "لا توجد أغاني لهذا الفنان",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (!isGridView) {
                // List Mode
                itemsIndexed(filteredSongs, key = { _, song -> song.id }) { _, song ->
                    ArtistSongRow(
                        song = song,
                        onClick = {
                            viewModel.playSong(song, filteredSongs)
                            onOpenPlayer()
                        },
                        onOptionsClick = {
                            selectedSongForSheet = song
                        }
                    )
                }
            } else {
                // Grid Mode
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(((filteredSongs.size + 1) / 2 * 200).dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { song ->
                            ArtistSongCard(
                                song = song,
                                onClick = {
                                    viewModel.playSong(song, filteredSongs)
                                    onOpenPlayer()
                                },
                                onOptionsClick = {
                                    selectedSongForSheet = song
                                }
                            )
                        }
                    }
                }
            }
        }

        selectedSongForSheet?.let { song ->
            SongOptionsSheet(
                song = song,
                sheetState = sheetState,
                onDismiss = { selectedSongForSheet = null },
                onPlayNow = {
                    viewModel.playSong(song, filteredSongs)
                    onOpenPlayer()
                },
                onAddToQueue = { viewModel.controller.addToQueue(song) },
                onNavigateToAlbum = { albumId -> onAlbumClick(albumId) },
                onNavigateToArtist = { }
            )
        }

        if (showSortSheet) {
            ArtistSortBottomSheet(
                sortBy = sortBy,
                sortAscending = sortAscending,
                isGridView = isGridView,
                onDismiss = { showSortSheet = false },
                onSortByChanged = { sortBy = it },
                onSortAscendingChanged = { sortAscending = it },
                onGridViewChanged = { isGridView = it }
            )
        }
    }
}

@Composable
private fun ArtistAlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.width(140.dp)
    ) {
        Column {
            AsyncImage(
                model = album.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.MusicNote),
                placeholder = rememberVectorPainter(Icons.Default.MusicNote),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${album.songCount} أغنية",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ArtistSongRow(
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
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            error = rememberVectorPainter(Icons.Default.MusicNote),
            placeholder = rememberVectorPainter(Icons.Default.MusicNote),
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
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
                text = song.album,
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
                contentDescription = "خيارات الأغنية",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArtistSongCard(
    song: Song,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Default.MusicNote),
                    placeholder = rememberVectorPainter(Icons.Default.MusicNote),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )

                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "خيارات",
                        tint = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.album,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistSortBottomSheet(
    sortBy: String,
    sortAscending: Boolean,
    isGridView: Boolean,
    onDismiss: () -> Unit,
    onSortByChanged: (String) -> Unit,
    onSortAscendingChanged: (Boolean) -> Unit,
    onGridViewChanged: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "خيارات الفرز والعرض",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // View Mode
            Text(
                text = "طريقة العرض",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = !isGridView,
                    onClick = { onGridViewChanged(false) },
                    label = { Text("قائمة") },
                    leadingIcon = { Icon(Icons.Default.ViewList, contentDescription = null) }
                )
                FilterChip(
                    selected = isGridView,
                    onClick = { onGridViewChanged(true) },
                    label = { Text("شبكة") },
                    leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sort By
            Text(
                text = "الفرز حسب",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = sortBy == "title",
                    onClick = { onSortByChanged("title") },
                    label = { Text("اسم الأغنية") }
                )
                FilterChip(
                    selected = sortBy == "duration",
                    onClick = { onSortByChanged("duration") },
                    label = { Text("المدة") }
                )
                FilterChip(
                    selected = sortBy == "date",
                    onClick = { onSortByChanged("date") },
                    label = { Text("تاريخ الإضافة") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Order
            Text(
                text = "الترتيب",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = sortAscending,
                    onClick = { onSortAscendingChanged(true) },
                    label = { Text("تصاعدي") }
                )
                FilterChip(
                    selected = !sortAscending,
                    onClick = { onSortAscendingChanged(false) },
                    label = { Text("تنازلي") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
