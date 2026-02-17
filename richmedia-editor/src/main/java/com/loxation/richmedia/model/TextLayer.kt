package com.loxation.richmedia.model

import kotlinx.serialization.Serializable

@Serializable
data class TextLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val position: LayerPosition = LayerPosition(),
    val style: TextLayerStyle = TextLayerStyle(),
    val animation: TextAnimation? = null,
    val path: AnimationPath? = null,
    val visible: Boolean = true,
    val zIndex: Int = 0,
    val lottieAnimation: LottieAnimation? = null
)
