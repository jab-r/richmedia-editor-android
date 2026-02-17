package com.loxation.richmedia.model

import kotlinx.serialization.Serializable

@Serializable
data class LottieAnimation(
    val jsonData: String,
    val name: String,
    val duration: Double,
    val frameRate: Double = 60.0,
    val loops: Boolean = false
)
