package com.loxation.richmedia.model

import kotlinx.serialization.Serializable

@Serializable
data class TextAnimation(
    val preset: AnimationPreset,
    val delay: Double = 0.0,
    val duration: Double = 0.8,
    val loop: Boolean = false,
    val loopDelay: Double = 0.0
)

@Serializable
enum class AnimationPreset {
    // Entrance
    fadeIn,
    fadeSlideUp,
    fadeSlideDown,
    fadeSlideLeft,
    fadeSlideRight,
    zoomIn,
    bounceIn,
    popIn,
    typewriter,
    blurIn,
    flipInX,
    flipInY,

    // Exit
    fadeOut,
    slideOutUp,
    slideOutDown,
    zoomOut,
    blurOut,
    shrinkOut,

    // Loop
    pulse,
    bounce,
    float,
    wiggle,
    rotate,
    glow,
    shake,
    heartbeat,
    colorCycle,
    swing,
    flash,

    // Path
    motionPath,
    curvePath;

    val displayName: String
        get() = when (this) {
            fadeIn -> "Fade In"
            fadeSlideUp -> "Slide Up"
            fadeSlideDown -> "Slide Down"
            fadeSlideLeft -> "Slide Left"
            fadeSlideRight -> "Slide Right"
            zoomIn -> "Zoom In"
            bounceIn -> "Bounce In"
            popIn -> "Pop In"
            typewriter -> "Typewriter"
            blurIn -> "Blur In"
            flipInX -> "Flip In X"
            flipInY -> "Flip In Y"
            fadeOut -> "Fade Out"
            slideOutUp -> "Slide Out Up"
            slideOutDown -> "Slide Out Down"
            zoomOut -> "Zoom Out"
            blurOut -> "Blur Out"
            shrinkOut -> "Shrink Out"
            pulse -> "Pulse"
            bounce -> "Bounce"
            float -> "Float"
            wiggle -> "Wiggle"
            rotate -> "Rotate"
            glow -> "Glow"
            shake -> "Shake"
            heartbeat -> "Heartbeat"
            colorCycle -> "Color Cycle"
            swing -> "Swing"
            flash -> "Flash"
            motionPath -> "Motion Path"
            curvePath -> "Curve Path"
        }

    val category: AnimationCategory
        get() = when (this) {
            fadeIn, fadeSlideUp, fadeSlideDown, fadeSlideLeft, fadeSlideRight,
            zoomIn, bounceIn, popIn, typewriter, blurIn, flipInX, flipInY -> AnimationCategory.ENTRANCE
            fadeOut, slideOutUp, slideOutDown, zoomOut, blurOut, shrinkOut -> AnimationCategory.EXIT
            pulse, bounce, float, wiggle, rotate, glow, shake, heartbeat,
            colorCycle, swing, flash -> AnimationCategory.LOOP
            motionPath, curvePath -> AnimationCategory.PATH
        }
}

enum class AnimationCategory(val displayName: String) {
    ENTRANCE("Entrance"),
    EXIT("Exit"),
    LOOP("Loop"),
    PATH("Path")
}
