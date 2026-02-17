package com.loxation.richmedia.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loxation.richmedia.model.LayerPosition
import com.loxation.richmedia.model.TextAlignment
import com.loxation.richmedia.model.TextLayer
import com.loxation.richmedia.service.AnimationRenderer
import com.loxation.richmedia.util.fromHex

/**
 * A single text layer rendered on the canvas.
 * Supports drag gestures in edit mode and animation playback in play mode.
 */
@Composable
internal fun TextLayerOverlay(
    layer: TextLayer,
    canvasSize: IntSize,
    isSelected: Boolean,
    isEditing: Boolean,
    isPlaying: Boolean,
    onSelected: () -> Unit,
    onPositionChanged: (LayerPosition) -> Unit
) {
    val density = LocalDensity.current
    val pos = layer.position
    val style = layer.style

    val offsetXPx = pos.x * canvasSize.width
    val offsetYPx = pos.y * canvasSize.height

    val animModifier = if (isPlaying) {
        AnimationRenderer.animatedModifier(layer)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .offset(
                x = with(density) { offsetXPx.toDp() },
                y = with(density) { offsetYPx.toDp() }
            )
            .graphicsLayer {
                scaleX = pos.scale
                scaleY = pos.scale
                rotationZ = pos.rotation
            }
            .then(animModifier)
            .then(
                if (isSelected) Modifier.border(1.dp, Color(0xFF2196F3))
                else Modifier
            )
            .then(
                if (isEditing) {
                    Modifier.pointerInput(layer.id) {
                        detectDragGestures(
                            onDragStart = { onSelected() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newX = (pos.x + dragAmount.x / canvasSize.width).coerceIn(0f, 1f)
                                val newY = (pos.y + dragAmount.y / canvasSize.height).coerceIn(0f, 1f)
                                onPositionChanged(pos.copy(x = newX, y = newY))
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        val textColor = runCatching { fromHex(style.color) }.getOrDefault(Color.White)
        val textDecoration = buildList {
            if (style.underline) add(TextDecoration.Underline)
            if (style.strikethrough) add(TextDecoration.LineThrough)
        }.let {
            if (it.isEmpty()) null else TextDecoration.combine(it)
        }

        Text(
            text = layer.text,
            style = TextStyle(
                color = textColor,
                fontSize = style.size.sp,
                fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = textDecoration,
                textAlign = when (style.align) {
                    TextAlignment.left -> TextAlign.Start
                    TextAlignment.center -> TextAlign.Center
                    TextAlignment.right -> TextAlign.End
                }
            )
        )
    }
}
