package com.loxation.richmedia.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import com.loxation.richmedia.model.LayerPosition
import com.loxation.richmedia.model.MediaTransform
import com.loxation.richmedia.model.RichPostBlock

/**
 * Single block canvas: background media + text layer overlays.
 * Handles pinch-to-zoom on background media and tap-to-select on layers.
 */
@Composable
internal fun MediaCanvasView(
    block: RichPostBlock,
    isPlaying: Boolean,
    isEditing: Boolean,
    localImages: Map<String, Bitmap>,
    selectedLayerId: String?,
    onLayerSelected: (layerId: String) -> Unit,
    onLayerPositionChanged: (layerId: String, LayerPosition) -> Unit,
    onMediaTransformChanged: (MediaTransform) -> Unit,
    onTapCanvas: () -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val transform = block.mediaTransform ?: MediaTransform()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { canvasSize = it }
            .pointerInput(isEditing) {
                if (isEditing) {
                    detectTapGestures { onTapCanvas() }
                }
            }
    ) {
        // Background media
        val localBitmap = localImages[block.id]
        if (localBitmap != null) {
            Image(
                bitmap = localBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = transform.scale
                        scaleY = transform.scale
                        translationX = transform.offsetX
                        translationY = transform.offsetY
                    }
            )
        } else if (block.url != null) {
            AsyncImage(
                model = block.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = transform.scale
                        scaleY = transform.scale
                        translationX = transform.offsetX
                        translationY = transform.offsetY
                    }
            )
        }

        // TODO: Lottie overlay (block.lottieOverlay)

        // Text layers sorted by zIndex
        block.textLayers
            ?.sortedBy { it.zIndex }
            ?.forEach { layer ->
                if (!layer.visible) return@forEach
                TextLayerOverlay(
                    layer = layer,
                    canvasSize = canvasSize,
                    isSelected = layer.id == selectedLayerId,
                    isEditing = isEditing,
                    isPlaying = isPlaying,
                    onSelected = { onLayerSelected(layer.id) },
                    onPositionChanged = { position -> onLayerPositionChanged(layer.id, position) }
                )
            }
    }
}
