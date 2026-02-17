package com.loxation.richmedia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
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
import com.loxation.richmedia.model.AnimationPreset
import com.loxation.richmedia.model.TextLayerStyle
import com.loxation.richmedia.service.AnimationRenderer
import com.loxation.richmedia.service.PathAnimationRenderer
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
    val viewConfiguration = LocalViewConfiguration.current
    val pos = layer.position
    val style = layer.style

    // Mutable gesture state — pixel coordinates during drag (like iOS),
    // converted back to normalized (0-1) on gesture end.
    var currentX by remember(pos.x) { mutableFloatStateOf(pos.x * canvasSize.width) }
    var currentY by remember(pos.y) { mutableFloatStateOf(pos.y * canvasSize.height) }
    var currentScale by remember(pos.scale) { mutableFloatStateOf(pos.scale) }
    var currentRotation by remember(pos.rotation) { mutableFloatStateOf(pos.rotation) }

    val animModifier = if (isPlaying) {
        AnimationRenderer.animatedModifier(layer)
    } else {
        Modifier
    }

    val pathModifier = if (isPlaying && layer.path != null &&
        (layer.animation?.preset == AnimationPreset.motionPath || layer.animation?.preset == AnimationPreset.curvePath)
    ) {
        val anim = layer.animation!!
        PathAnimationRenderer.pathModifier(
            path = layer.path,
            canvasSize = canvasSize,
            durationMs = (anim.duration * 1000).toInt(),
            delayMs = (anim.delay * 1000).toInt(),
            loop = anim.loop
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .offset(
                x = with(density) { currentX.toDp() },
                y = with(density) { currentY.toDp() }
            )
            .graphicsLayer {
                scaleX = currentScale
                scaleY = currentScale
                rotationZ = currentRotation
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
            }
            .then(animModifier)
            .then(pathModifier)
            .then(
                if (isSelected) Modifier.border(2.dp, Color(0xFF2196F3), RoundedCornerShape(4.dp))
                else Modifier
            )
            .then(
                if (isEditing) {
                    // Single pointerInput handles tap, long press, and drag
                    // to avoid conflicts between separate gesture detectors.
                    // Matches iOS pattern: drag uses absolute position tracking.
                    Modifier.pointerInput(layer.id) {
                        val touchSlop = viewConfiguration.touchSlop
                        val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            val downPosition = down.position
                            var isDragging = false
                            var longPressFired = false
                            val downTime = System.currentTimeMillis()

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break

                                if (change.changedToUp()) {
                                    change.consume()
                                    if (!isDragging && !longPressFired) {
                                        onSelected()
                                    }
                                    if (isDragging) {
                                        val nx = (currentX / canvasSize.width).coerceIn(0f, 1f)
                                        val ny = (currentY / canvasSize.height).coerceIn(0f, 1f)
                                        onPositionChanged(pos.copy(
                                            x = nx, y = ny,
                                            scale = currentScale,
                                            rotation = currentRotation
                                        ))
                                    }
                                    break
                                }

                                if (!isDragging && !longPressFired) {
                                    val elapsed = System.currentTimeMillis() - downTime
                                    if (elapsed >= longPressTimeoutMs) {
                                        longPressFired = true
                                        onSelected()
                                        onLongPress?.invoke()
                                    }
                                }

                                // Check total distance from initial touch point
                                val totalDist = (change.position - downPosition).getDistance()
                                if (!isDragging && totalDist > touchSlop) {
                                    isDragging = true
                                    onSelected()
                                }

                                if (isDragging) {
                                    change.consume()
                                    val delta = change.positionChange()
                                    currentX += delta.x
                                    currentY += delta.y
                                }
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        StyledText(layer = layer, style = style)
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
