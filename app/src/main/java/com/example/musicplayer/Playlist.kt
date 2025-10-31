package com.example.musicplayer

// Represents a user-created playlist
data class Playlist(
    val id: Long,
    val name: String,
    val songs: List<Song>
)
