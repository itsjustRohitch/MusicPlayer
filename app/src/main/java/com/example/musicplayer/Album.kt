package com.example.musicplayer

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtUri: Uri?
) : Parcelable
