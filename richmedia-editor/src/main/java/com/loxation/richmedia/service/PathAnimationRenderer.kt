package com.loxation.richmedia.service

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import com.loxation.richmedia.model.*
import kotlin.math.*

object PathAnimationRenderer {

    @Composable
    fun pathModifier(
        path: AnimationPath,
        canvasSize: IntSize,
        durationMs: Int,
        delayMs: Int,
        loop: Boolean
    ): Modifier {
        if (path.points.size < 2 || canvasSize.width == 0 || canvasSize.height == 0) {
            return Modifier
        }

        val progress = if (loop) {
            val infiniteTransition = rememberInfiniteTransition(label = "pathLoop")
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pathProgress"
            )
        } else {
            val animatable = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(delayMs.toLong())
                animatable.animateTo(1f, animationSpec = tween(durationMs, easing = LinearEasing))
            }
            animatable.asState()
        }

        val point = interpolateAlongPath(path, progress.value)

        // Point is in normalized 0-1 space, convert to pixel offset relative to center of path
        val centerX = path.points.map { it.x }.average().toFloat()
        val centerY = path.points.map { it.y }.average().toFloat()
        val offsetX = (point.x - centerX) * canvasSize.width
        val offsetY = (point.y - centerY) * canvasSize.height

        return Modifier.graphicsLayer {
            translationX = offsetX
            translationY = offsetY
        }
    }

    private fun interpolateAlongPath(path: AnimationPath, progress: Float): PathPoint {
        return when (path.type) {
            PathType.linear -> linearInterpolate(path.points, progress)
            PathType.bezier -> when (path.curveType) {
                CurveType.quadratic -> quadraticBezier(path.points, progress)
                CurveType.cubic -> cubicBezier(path.points, progress)
            }
            PathType.circular -> circularInterpolate(path.points, progress)
            PathType.arc -> arcInterpolate(path.points, progress)
            PathType.wave -> linearInterpolate(path.points, progress)
            PathType.custom -> when (path.curveType) {
                CurveType.quadratic -> catmullRomInterpolate(path.points, progress)
                CurveType.cubic -> catmullRomInterpolate(path.points, progress)
            }
        }
    }

    private fun linearInterpolate(points: List<PathPoint>, progress: Float): PathPoint {
        if (points.size < 2) return points.firstOrNull() ?: PathPoint(0.5f, 0.5f)

        // Calculate cumulative segment lengths for uniform speed
        val segments = mutableListOf(0f)
        for (i in 1 until points.size) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            segments.add(segments.last() + sqrt(dx * dx + dy * dy))
        }
        val totalLength = segments.last()
        if (totalLength == 0f) return points.first()

        val targetLength = progress * totalLength
        var segIdx = 0
        for (i in 1 until segments.size) {
            if (segments[i] >= targetLength) {
                segIdx = i - 1
                break
            }
            segIdx = i - 1
        }

        val segStart = segments[segIdx]
        val segEnd = segments[segIdx + 1]
        val segProgress = if (segEnd > segStart) (targetLength - segStart) / (segEnd - segStart) else 0f

        val p0 = points[segIdx]
        val p1 = points[segIdx + 1]
        return PathPoint(
            x = p0.x + (p1.x - p0.x) * segProgress,
            y = p0.y + (p1.y - p0.y) * segProgress
        )
    }

    private fun quadraticBezier(points: List<PathPoint>, progress: Float): PathPoint {
        if (points.size < 3) return linearInterpolate(points, progress)
        val t = progress.coerceIn(0f, 1f)
        val p0 = points.first()
        val p1 = points[points.size / 2]
        val p2 = points.last()
        val oneMinusT = 1f - t
        return PathPoint(
            x = oneMinusT * oneMinusT * p0.x + 2 * oneMinusT * t * p1.x + t * t * p2.x,
            y = oneMinusT * oneMinusT * p0.y + 2 * oneMinusT * t * p1.y + t * t * p2.y
        )
    }

    private fun cubicBezier(points: List<PathPoint>, progress: Float): PathPoint {
        if (points.size < 4) return quadraticBezier(points, progress)
        val t = progress.coerceIn(0f, 1f)
        val p0 = points.first()
        val p1 = points[points.size / 3]
        val p2 = points[2 * points.size / 3]
        val p3 = points.last()
        val oneMinusT = 1f - t
        return PathPoint(
            x = oneMinusT.pow(3) * p0.x + 3 * oneMinusT.pow(2) * t * p1.x +
                    3 * oneMinusT * t.pow(2) * p2.x + t.pow(3) * p3.x,
            y = oneMinusT.pow(3) * p0.y + 3 * oneMinusT.pow(2) * t * p1.y +
                    3 * oneMinusT * t.pow(2) * p2.y + t.pow(3) * p3.y
        )
    }

    private fun circularInterpolate(points: List<PathPoint>, progress: Float): PathPoint {
        if (points.size < 3) return linearInterpolate(points, progress)
        val cx = points.map { it.x }.average().toFloat()
        val cy = points.map { it.y }.average().toFloat()
        val radius = points.map { sqrt((it.x - cx).pow(2) + (it.y - cy).pow(2)) }.average().toFloat()
        val angle = progress * 2f * PI.toFloat()
        return PathPoint(
            x = cx + radius * cos(angle),
            y = cy + radius * sin(angle)
        )
    }

    private fun arcInterpolate(points: List<PathPoint>, progress: Float): PathPoint {
        if (points.size < 3) return linearInterpolate(points, progress)
        val cx = points.map { it.x }.average().toFloat()
        val cy = points.map { it.y }.average().toFloat()
        val radius = points.map { sqrt((it.x - cx).pow(2) + (it.y - cy).pow(2)) }.average().toFloat()
        val startAngle = atan2(points.first().y - cy, points.first().x - cx)
        val endAngle = atan2(points.last().y - cy, points.last().x - cx)
        val angle = startAngle + (endAngle - startAngle) * progress
        return PathPoint(
            x = cx + radius * cos(angle),
            y = cy + radius * sin(angle)
        )
    }

    private fun catmullRomInterpolate(points: List<PathPoint>, progress: Float): PathPoint {
        if (points.size < 4) return linearInterpolate(points, progress)
        val t = progress.coerceIn(0f, 1f)
        val numSegments = points.size - 1
        val scaledT = t * numSegments
        val segIndex = scaledT.toInt().coerceIn(0, numSegments - 1)
        val localT = scaledT - segIndex

        val p0 = points[(segIndex - 1).coerceAtLeast(0)]
        val p1 = points[segIndex]
        val p2 = points[(segIndex + 1).coerceAtMost(points.size - 1)]
        val p3 = points[(segIndex + 2).coerceAtMost(points.size - 1)]

        val tt = localT * localT
        val ttt = tt * localT

        return PathPoint(
            x = 0.5f * ((2f * p1.x) +
                    (-p0.x + p2.x) * localT +
                    (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * tt +
                    (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * ttt),
            y = 0.5f * ((2f * p1.y) +
                    (-p0.y + p2.y) * localT +
                    (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * tt +
                    (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * ttt)
        )
    }

    private fun Float.pow(n: Int): Float {
        var result = 1f
        repeat(n) { result *= this }
        return result
    }
}
