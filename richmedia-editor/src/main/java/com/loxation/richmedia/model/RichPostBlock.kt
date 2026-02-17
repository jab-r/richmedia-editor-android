package com.loxation.richmedia.model

import kotlinx.serialization.Serializable

@Serializable
data class RichPostBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val image: String? = null,
    val video: String? = null,
    val url: String? = null,
    val caption: String? = null,
    val textLayers: List<TextLayer>? = null,
    val animationVersion: Int? = null,
    val lottieOverlay: LottieAnimation? = null,
    val mediaTransform: MediaTransform? = null
) {
    val blockType: BlockType
        get() = when {
            video != null -> BlockType.VIDEO
            image != null -> BlockType.IMAGE
            else -> BlockType.TEXT
        }
}

enum class BlockType {
    TEXT, IMAGE, VIDEO
}
