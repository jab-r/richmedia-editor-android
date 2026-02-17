package com.loxation.richmedia.model

import kotlinx.serialization.Serializable

@Serializable
data class AnimationPath(
    val type: PathType,
    val points: List<PathPoint> = emptyList(),
    val curveType: CurveType = CurveType.quadratic
)

@Serializable
enum class PathType {
    linear,
    bezier,
    circular,
    arc,
    wave,
    custom
}

@Serializable
enum class CurveType {
    quadratic,
    cubic
}

/** Normalized point (0.0–1.0) within the canvas. */
@Serializable
data class PathPoint(
    val x: Float,
    val y: Float
)
