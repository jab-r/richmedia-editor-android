package com.loxation.richmedia.model

import kotlinx.serialization.Serializable

/**
 * Zoom/pan framing for background media.
 * Scale range: 1.0–5.0. Offsets are clamped so media always covers the canvas.
 * Clamping formula: maxOffset = canvasSize * (scale - 1) / 2
 */
@Serializable
data class MediaTransform(
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    fun clampedOffset(canvasWidth: Float, canvasHeight: Float): MediaTransform {
        val maxOffsetX = canvasWidth * (scale - 1) / 2
        val maxOffsetY = canvasHeight * (scale - 1) / 2
        return copy(
            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX),
            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
        )
    }
}
