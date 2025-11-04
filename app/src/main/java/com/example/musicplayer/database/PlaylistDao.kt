package com.example.musicplayer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity)

    @Insert
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    // --- NEW ---
    // Deletes a specific song-playlist combination
    @Delete
    suspend fun removeSongFromPlaylist(crossRef: PlaylistSongCrossRef)
    // --- END NEW ---

    // THIS QUERY IS NOW UPDATED
    // It returns a list of the new 'PlaylistWithSongRefs' object.
    @Transaction
    @Query("SELECT * FROM playlists")
    fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongRefs>>
}

