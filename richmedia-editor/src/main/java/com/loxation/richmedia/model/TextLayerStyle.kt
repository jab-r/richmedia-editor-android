package com.loxation.richmedia.model

import kotlinx.serialization.Serializable

@Serializable
data class TextLayerStyle(
    val font: String = "System",
    val size: Float = 32f,
    val color: String = "#FFFFFF",
    val backgroundColor: String? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val align: TextAlignment = TextAlignment.center,
    val shadow: TextShadow? = null,
    val outline: TextOutline? = null
)

@Serializable
enum class TextAlignment {
    left, center, right
}

@Serializable
data class TextShadow(
    val color: String = "#000000",
    val opacity: Float = 0.5f,
    val radius: Float = 4f,
    val offset: ShadowOffset = ShadowOffset()
)

@Serializable
data class ShadowOffset(
    val width: Float = 0f,
    val height: Float = 2f
)

@Serializable
data class TextOutline(
    val color: String = "#000000",
    val width: Float = 2f
)
