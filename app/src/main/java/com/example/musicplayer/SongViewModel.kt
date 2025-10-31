package com.example.musicplayer

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.core.net.toUri


class SongViewModel(application: Application) : AndroidViewModel(application) {

    // -------------------------------
    // Music Data
    // -------------------------------
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    private val _folders = MutableStateFlow<List<MusicFolder>>(emptyList())
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()
    val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // -------------------------------
    // Playback State
    // -------------------------------
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(PlaybackService.RepeatMode.OFF)
    val repeatMode: StateFlow<PlaybackService.RepeatMode> = _repeatMode.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private var mediaController: MediaController? = null

    // -------------------------------
    // Filtered Lists (Search)
    // -------------------------------
    val filteredSongs: StateFlow<List<Song>> = combine(_songs, _searchQuery) { songs, query ->
        songs.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredAlbums: StateFlow<List<Album>> = combine(_albums, _searchQuery) { albums, query ->
        albums.filter { it.title.contains(query, true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredFolders: StateFlow<List<MusicFolder>> = combine(_folders, _searchQuery) { folders, query ->
        folders.filter { it.name.contains(query, true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // -------------------------------
    // Initialization
    // -------------------------------
    init {
        loadSongs()
        connectToPlaybackService()
    }

    // -------------------------------
    // Media Controller Setup
    // -------------------------------
    private fun connectToPlaybackService() {
        val sessionToken = SessionToken(getApplication(), ComponentName(getApplication(), PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()

        // Use ContextCompat.getMainExecutor to avoid API 28+ restriction
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                mediaController?.addListener(playerListener)
                updateState()
            },
            ContextCompat.getMainExecutor(getApplication())
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            updateState()
        }
    }

    // -------------------------------
    // Update ViewModel State
    // -------------------------------
    private fun updateState() {
        val controller = mediaController ?: return

        _isPlaying.value = controller.isPlaying
        _shuffleModeEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = PlaybackService.RepeatMode.fromExoMode(controller.repeatMode)

        val currentMediaItem = controller.currentMediaItem
        _currentSong.value = currentMediaItem?.let { item ->
            songs.value.find { it.contentUri.toString() == item.mediaId }
        }
    }

    // -------------------------------
    // Playback Controls
    // -------------------------------
    fun setPlayQueue(songs: List<Song>, startIndex: Int = 0) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.contentUri)
                .setMediaId(song.contentUri.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(song.albumArtUri)
                        .build()
                )
                .build()
        }
        mediaController?.apply {
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            play()
        }
    }

    fun playPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = mediaController?.seekToNext()

    fun previous() = mediaController?.seekToPrevious()

    fun seekTo(position: Long) = mediaController?.seekTo(position)

    fun toggleShuffleMode() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
            _shuffleModeEnabled.value = it.shuffleModeEnabled
        }
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (_repeatMode.value) {
            PlaybackService.RepeatMode.OFF -> PlaybackService.RepeatMode.ALL
            PlaybackService.RepeatMode.ALL -> PlaybackService.RepeatMode.ONE
            PlaybackService.RepeatMode.ONE -> PlaybackService.RepeatMode.OFF
        }
        _repeatMode.value = nextMode
        controller.repeatMode = nextMode.exoMode
    }

    // -------------------------------
    // Search & Playlist
    // -------------------------------
    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun getSongsForAlbum(albumId: Long) = _songs.value.filter { it.albumId == albumId }

    fun getSongsForFolder(folderPath: String) = _songs.value.filter { it.filePath.startsWith(folderPath) }

    fun createPlaylist(name: String) {
        val newPlaylist = Playlist(System.currentTimeMillis(), name, emptyList())
        _playlists.update { it + newPlaylist }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        _playlists.update { playlists ->
            playlists.map { if (it.id == playlistId) it.copy(songs = it.songs + song) else it }
        }
    }

    fun getSongsForPlaylist(playlistId: Long) = _playlists.value.find { it.id == playlistId }?.songs ?: emptyList()

    // -------------------------------
    // Load Local Music
    // -------------------------------
    fun loadSongs() {
        viewModelScope.launch {
            val songList = mutableListOf<Song>()
            val albumMap = mutableMapOf<Long, Album>()
            val folderMap = mutableMapOf<String, MusicFolder>()

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            val query = getApplication<Application>().contentResolver.query(collection, projection, selection, null, null)
            query?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)
                    val artist = cursor.getString(artistCol)
                    val album = cursor.getString(albumCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val data = cursor.getString(dataCol)
                    val duration = cursor.getLong(durationCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val albumArtUri = "content://media/external/audio/albumart/$albumId".toUri()
                    val folderPath = data.substringBeforeLast('/')

                    val song = Song(id, title, artist, contentUri, albumArtUri, albumId, folderPath, duration)
                    songList.add(song)
                    albumMap.putIfAbsent(albumId, Album(albumId, album, artist, albumArtUri))
                    folderMap.putIfAbsent(folderPath, MusicFolder(folderPath, folderPath.substringAfterLast('/')))
                }
            }

            _songs.value = songList
            _albums.value = albumMap.values.toList()
            _folders.value = folderMap.values.toList()
        }

        // Update playback position every second
        viewModelScope.launch {
            while (true) {
                _playbackPosition.value = mediaController?.currentPosition ?: 0
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.removeListener(playerListener)
        mediaController = null
    }
}
