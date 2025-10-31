package com.example.musicplayer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MusicFolder(
    val name: String,
    val path: String
) : Parcelable
