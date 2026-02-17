package com.loxation.richmedia.model

import kotlinx.serialization.Serializable

/**
 * Normalized position within the 9:16 canvas.
 * (0,0) = top-left, (1,1) = bottom-right. Position specifies center point.
 */
@Serializable
data class LayerPosition(
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val rotation: Float = 0f,
    val scale: Float = 1.0f
)
