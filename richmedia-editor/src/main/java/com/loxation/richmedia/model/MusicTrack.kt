package com.loxation.richmedia.model

import kotlinx.serialization.Serializable

@Serializable
data class MusicTrack(
    val trackName: String,
    val artistName: String,
    val albumName: String? = null,
    val previewURL: String,
    val artworkURL: String? = null,
    val appleMusicID: String
)
