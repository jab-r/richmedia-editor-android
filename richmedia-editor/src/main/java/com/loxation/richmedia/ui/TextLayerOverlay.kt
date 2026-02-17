package com.loxation.richmedia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import com.loxation.richmedia.model.TextLayerStyle
import com.loxation.richmedia.service.AnimationRenderer
import com.loxation.richmedia.util.fromHex

/**
 * A single text layer rendered on the canvas.
 * Supports drag/pinch/rotate gestures in edit mode and animation playback in play mode.
 */
@Composable
internal fun TextLayerOverlay(
    layer: TextLayer,
    canvasSize: IntSize,
    isSelected: Boolean,
    isEditing: Boolean,
    isPlaying: Boolean,
    onSelected: () -> Unit,
    onPositionChanged: (LayerPosition) -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val pos = layer.position
    val style = layer.style

    val offsetXPx = pos.x * canvasSize.width
    val offsetYPx = pos.y * canvasSize.height

    // Mutable gesture state for smooth interaction
    var currentScale by remember(pos.scale) { mutableFloatStateOf(pos.scale) }
    var currentRotation by remember(pos.rotation) { mutableFloatStateOf(pos.rotation) }

    val animModifier = if (isPlaying) {
        AnimationRenderer.animatedModifier(layer)
    } else {
        Modifier
    }

    // Pinch-to-scale and rotation gesture
    val transformableState = rememberTransformableState { zoomChange, _, rotationChange ->
        currentScale = (currentScale * zoomChange).coerceIn(0.5f, 3.0f)
        currentRotation = (currentRotation + rotationChange) % 360f
    }

    Box(
        modifier = Modifier
            .offset(
                x = with(density) { offsetXPx.toDp() },
                y = with(density) { offsetYPx.toDp() }
            )
            .graphicsLayer {
                scaleX = currentScale
                scaleY = currentScale
                rotationZ = currentRotation
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
            }
            .then(animModifier)
            .then(
                if (isSelected) Modifier.border(2.dp, Color(0xFF2196F3), RoundedCornerShape(4.dp))
                else Modifier
            )
            .then(
                if (isEditing) {
                    Modifier
                        .pointerInput(layer.id) {
                            detectTapGestures(
                                onTap = { onSelected() },
                                onLongPress = { onLongPress?.invoke() }
                            )
                        }
                        .pointerInput(layer.id) {
                            detectDragGestures(
                                onDragStart = { onSelected() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newX = (pos.x + dragAmount.x / canvasSize.width).coerceIn(0f, 1f)
                                    val newY = (pos.y + dragAmount.y / canvasSize.height).coerceIn(0f, 1f)
                                    onPositionChanged(pos.copy(x = newX, y = newY))
                                },
                                onDragEnd = {
                                    // Commit scale/rotation changes
                                    onPositionChanged(pos.copy(scale = currentScale, rotation = currentRotation))
                                }
                            )
                        }
                        .transformable(state = transformableState, lockRotationOnZoomPan = false)
                } else Modifier
            )
            .then(
                if (isEditing) {
                    // On gesture end, commit scale/rotation
                    Modifier
                } else Modifier
            )
    ) {
        StyledText(layer = layer, style = style)
    }

    // Commit scale/rotation changes when gestures end
    LaunchedEffect(currentScale, currentRotation) {
        if (isEditing && (currentScale != pos.scale || currentRotation != pos.rotation)) {
            onPositionChanged(pos.copy(scale = currentScale, rotation = currentRotation))
        }
    }
}

@Composable
private fun StyledText(layer: TextLayer, style: TextLayerStyle) {
    val textColor = runCatching { fromHex(style.color) }.getOrDefault(Color.White)
    val textDecoration = buildList {
        if (style.underline) add(TextDecoration.Underline)
        if (style.strikethrough) add(TextDecoration.LineThrough)
    }.let {
        if (it.isEmpty()) null else TextDecoration.combine(it)
    }

    val fontFamily = when (style.font) {
        "Georgia" -> FontFamily.Serif
        "Helvetica" -> FontFamily.SansSerif
        "Courier" -> FontFamily.Monospace
        "Times New Roman" -> FontFamily.Serif
        else -> FontFamily.Default
    }

    val shadow = style.shadow?.let { s ->
        val shadowColor = runCatching { fromHex(s.color) }.getOrDefault(Color.Black)
        Shadow(
            color = shadowColor.copy(alpha = s.opacity),
            offset = Offset(s.offset.width, s.offset.height),
            blurRadius = s.radius
        )
    }

    val bgColor = style.backgroundColor?.let {
        runCatching { fromHex(it) }.getOrNull()
    }

    val bgModifier = if (bgColor != null) {
        Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    } else {
        Modifier
    }

    // Outline text: draw stroke behind the main text
    if (style.outline != null) {
        val outlineColor = runCatching { fromHex(style.outline.color) }.getOrDefault(Color.Black)
        Box(modifier = bgModifier) {
            // Stroke text (behind)
            Text(
                text = layer.text,
                style = TextStyle(
                    color = outlineColor,
                    fontSize = style.size.sp,
                    fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                    fontFamily = fontFamily,
                    textAlign = when (style.align) {
                        TextAlignment.left -> TextAlign.Start
                        TextAlignment.center -> TextAlign.Center
                        TextAlignment.right -> TextAlign.End
                    },
                    drawStyle = Stroke(width = style.outline.width * 2),
                    shadow = shadow
                )
            )
            // Fill text (on top)
            Text(
                text = layer.text,
                style = TextStyle(
                    color = textColor,
                    fontSize = style.size.sp,
                    fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = textDecoration,
                    fontFamily = fontFamily,
                    textAlign = when (style.align) {
                        TextAlignment.left -> TextAlign.Start
                        TextAlignment.center -> TextAlign.Center
                        TextAlignment.right -> TextAlign.End
                    }
                )
            )
        }
    } else {
        Text(
            text = layer.text,
            modifier = bgModifier,
            style = TextStyle(
                color = textColor,
                fontSize = style.size.sp,
                fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = textDecoration,
                fontFamily = fontFamily,
                textAlign = when (style.align) {
                    TextAlignment.left -> TextAlign.Start
                    TextAlignment.center -> TextAlign.Center
                    TextAlignment.right -> TextAlign.End
                },
                shadow = shadow
            )
        )
    }
}
