package com.loxation.richmedia.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.loxation.richmedia.model.AnimationPath
import com.loxation.richmedia.model.CurveType
import com.loxation.richmedia.model.PathPoint
import com.loxation.richmedia.model.PathType
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathDrawingView(
    onComplete: (AnimationPath) -> Unit,
    onDismiss: () -> Unit
) {
    var rawPoints by remember { mutableStateOf(listOf<Offset>()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Top bar: Cancel | Title | Done
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text("Draw Path", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(
                    onClick = {
                        if (rawPoints.size >= 3 && canvasSize.width > 0) {
                            onComplete(buildPath(rawPoints, canvasSize))
                        }
                    },
                    enabled = rawPoints.size >= 3
                ) { Text("Done") }
            }

            // Drawing canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .onSizeChanged { canvasSize = it }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    rawPoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    rawPoints = rawPoints + change.position
                                }
                            )
                        }
                ) {
                    if (rawPoints.isEmpty()) return@Canvas

                    // Draw path line
                    val path = Path()
                    rawPoints.forEachIndexed { index, point ->
                        if (index == 0) path.moveTo(point.x, point.y)
                        else path.lineTo(point.x, point.y)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF2196F3),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw control point dots
                    val simplified = simplifyPoints(rawPoints)
                    simplified.forEach { point ->
                        drawCircle(
                            color = Color(0xFF2196F3),
                            radius = 4.dp.toPx(),
                            center = point
                        )
                    }
                }

                // Instructions overlay when empty
                if (rawPoints.isEmpty()) {
                    Text(
                        "Draw a path with your finger",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bottom presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { rawPoints = emptyList() },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear") }
                OutlinedButton(
                    onClick = {
                        if (canvasSize.width > 0) {
                            rawPoints = generateCirclePoints(canvasSize)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Circle") }
                OutlinedButton(
                    onClick = {
                        if (canvasSize.width > 0) {
                            rawPoints = generateWavePoints(canvasSize)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Wave") }
                OutlinedButton(
                    onClick = {
                        if (canvasSize.width > 0) {
                            rawPoints = generateArcPoints(canvasSize)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Arc") }
            }
        }
    }
}

private fun simplifyPoints(points: List<Offset>): List<Offset> {
    if (points.size <= 10) return points
    val step = points.size / 10
    return points.filterIndexed { index, _ -> index % step == 0 || index == points.size - 1 }
}

private fun buildPath(rawPoints: List<Offset>, canvasSize: IntSize): AnimationPath {
    // Simplify: keep every 5th point when > 10
    val simplified = if (rawPoints.size > 10) {
        val step = 5
        rawPoints.filterIndexed { index, _ -> index % step == 0 || index == rawPoints.size - 1 }
    } else {
        rawPoints
    }

    // Normalize to 0-1 range
    val normalized = simplified.map { offset ->
        PathPoint(
            x = (offset.x / canvasSize.width).coerceIn(0f, 1f),
            y = (offset.y / canvasSize.height).coerceIn(0f, 1f)
        )
    }

    return AnimationPath(
        type = PathType.custom,
        points = normalized,
        curveType = CurveType.quadratic
    )
}

private fun generateCirclePoints(canvasSize: IntSize): List<Offset> {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val radius = min(cx, cy) * 0.6f
    return (0..12).map { i ->
        val angle = (2.0 * PI * i / 12).toFloat()
        Offset(cx + radius * cos(angle), cy + radius * sin(angle))
    }
}

private fun generateWavePoints(canvasSize: IntSize): List<Offset> {
    val w = canvasSize.width.toFloat()
    val h = canvasSize.height.toFloat()
    val amplitude = h * 0.15f
    val cy = h / 2f
    return (0..20).map { i ->
        val t = i / 20f
        Offset(
            x = w * 0.1f + w * 0.8f * t,
            y = cy + amplitude * sin(t * 4f * PI.toFloat())
        )
    }
}

private fun generateArcPoints(canvasSize: IntSize): List<Offset> {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val radius = min(cx, cy) * 0.6f
    return (0..10).map { i ->
        val angle = (PI / 2.0 * i / 10).toFloat() // quarter circle
        Offset(cx + radius * cos(angle), cy - radius * sin(angle))
    }
}
