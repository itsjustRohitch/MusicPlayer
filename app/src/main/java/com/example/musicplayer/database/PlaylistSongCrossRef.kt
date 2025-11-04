package com.example.musicplayer.database

import androidx.room.Entity

// This table just stores pairs of IDs: (playlistId, songId)
@Entity(tableName = "playlist_song_cross_ref", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long // This is the song ID from MediaStore
)