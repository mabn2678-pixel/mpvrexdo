package com.finalplayer.app.music.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.finalplayer.app.music.data.model.MusicPlayerState
import com.finalplayer.app.music.data.model.Song
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(UnstableApi::class)
class MusicController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()
    val playerState: StateFlow<MusicPlayerState> get() = state

    suspend fun connect() {
        if (mediaController != null) return
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        mediaController = suspendCancellableCoroutine { continuation ->
            future.addListener({
                try {
                    val controller = future.get()
                    setupPlayerListener(controller)
                    startPolling()
                    if (continuation.isActive) continuation.resume(controller)
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }, MoreExecutors.directExecutor())

            continuation.invokeOnCancellation {
                MediaController.releaseFuture(future)
            }
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                updateStateFromController()
                delay(200)
            }
        }
    }

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                updateStateFromController()
            }
        })
    }

    private fun updateStateFromController() {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        val queue = _state.value.queue
        val currentSong = if (currentIndex in queue.indices) queue[currentIndex] else _state.value.currentSong

        _state.value = _state.value.copy(
            currentSong = currentSong,
            isPlaying = controller.isPlaying,
            positionMs = controller.currentPosition.coerceAtLeast(0L),
            durationMs = controller.duration.coerceAtLeast(0L),
            repeatMode = controller.repeatMode,
            shuffleEnabled = controller.shuffleModeEnabled,
            currentQueueIndex = currentIndex
        )
    }

    fun play(songs: List<Song>, startIndex: Int = 0) {
        val controller = mediaController ?: return
        if (songs.isEmpty()) return

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.albumArtUri)
                        .build()
                )
                .build()
        }

        val validIndex = startIndex.coerceIn(0, songs.lastIndex)
        _state.value = _state.value.copy(
            queue = songs,
            currentSong = songs[validIndex],
            currentQueueIndex = validIndex
        )

        controller.setMediaItems(mediaItems, validIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val index = queue.indexOf(song).coerceAtLeast(0)
        play(queue, index)
    }

    fun resume() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun setRepeatMode(mode: Int) {
        mediaController?.repeatMode = mode
        _state.value = _state.value.copy(repeatMode = mode)
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffle = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newShuffle
        _state.value = _state.value.copy(shuffleEnabled = newShuffle)
    }

    fun addToQueue(song: Song) {
        val controller = mediaController ?: return
        val currentQueue = _state.value.queue.toMutableList()
        currentQueue.add(song)

        val mediaItem = MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.albumArtUri)
                    .build()
            )
            .build()

        controller.addMediaItem(mediaItem)
        _state.value = _state.value.copy(queue = currentQueue)
    }

    fun skipToQueueItem(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekTo(index, 0L)
        }
    }

    fun getCurrentPosition(): Long {
        return mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
    }

    fun getDuration(): Long {
        return mediaController?.duration?.coerceAtLeast(0L) ?: 0L
    }
}
