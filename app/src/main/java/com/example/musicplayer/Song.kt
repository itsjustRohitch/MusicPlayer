package com.example.musicplayer

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val albumId: Long,
    val filePath: String,
    val duration: Long
) : Parcelable

