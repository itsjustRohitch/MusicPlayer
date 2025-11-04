package com.example.musicplayer.database

import androidx.room.Embedded
import androidx.room.Relation


data class PlaylistWithSongRefs(
    @Embedded
    val playlist: PlaylistEntity,

    @Relation(
        parentColumn = "playlistId", // The "primary key" in PlaylistEntity
        entityColumn = "playlistId"  // The "foreign key" in PlaylistSongCrossRef
    )
    val songRefs: List<PlaylistSongCrossRef>
)
