package com.example.musicplayer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector, val title: String) {
    object Songs : Screen("songs", Icons.Default.MusicNote, "Songs")
    object Albums : Screen("albums", Icons.Default.Album, "Albums")
    object Folders : Screen("folders", Icons.Default.Folder, "Folders")
    object Playlists : Screen("playlists", Icons.AutoMirrored.Filled.List, "Playlists")
}
