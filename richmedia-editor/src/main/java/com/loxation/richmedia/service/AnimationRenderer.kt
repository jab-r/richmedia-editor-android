package com.loxation.richmedia.service

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.loxation.richmedia.model.AnimationPreset
import com.loxation.richmedia.model.TextLayer

/**
 * Applies the configured animation preset to a Composable.
 * Returns a Modifier chain with the animation effects.
 */
object AnimationRenderer {

    @Composable
    fun animatedModifier(layer: TextLayer): Modifier {
        val animation = layer.animation ?: return Modifier
        val preset = animation.preset
        val durationMs = (animation.duration * 1000).toInt()
        val delayMs = (animation.delay * 1000).toInt()

        return when (preset) {
            AnimationPreset.fadeIn -> fadeInModifier(durationMs, delayMs)
            AnimationPreset.fadeSlideUp -> fadeSlideModifier(durationMs, delayMs, yOffset = 50f)
            AnimationPreset.fadeSlideDown -> fadeSlideModifier(durationMs, delayMs, yOffset = -50f)
            AnimationPreset.fadeSlideLeft -> fadeSlideModifier(durationMs, delayMs, xOffset = 50f)
            AnimationPreset.fadeSlideRight -> fadeSlideModifier(durationMs, delayMs, xOffset = -50f)
            AnimationPreset.zoomIn -> zoomInModifier(durationMs, delayMs)
            AnimationPreset.bounceIn -> bounceInModifier(durationMs, delayMs)
            AnimationPreset.popIn -> popInModifier(durationMs, delayMs)
            AnimationPreset.typewriter -> typewriterModifier(layer.text, durationMs, delayMs)
            AnimationPreset.blurIn -> blurInModifier(durationMs, delayMs)
            AnimationPreset.flipInX -> flipInModifier(durationMs, delayMs, axisX = true)
            AnimationPreset.flipInY -> flipInModifier(durationMs, delayMs, axisX = false)
            AnimationPreset.fadeOut -> fadeOutModifier(durationMs, delayMs)
            AnimationPreset.slideOutUp -> slideOutModifier(durationMs, delayMs, yTarget = -100f)
            AnimationPreset.slideOutDown -> slideOutModifier(durationMs, delayMs, yTarget = 100f)
            AnimationPreset.zoomOut -> zoomOutModifier(durationMs, delayMs)
            AnimationPreset.blurOut -> blurOutModifier(durationMs, delayMs)
            AnimationPreset.shrinkOut -> shrinkOutModifier(durationMs, delayMs)
            AnimationPreset.pulse -> pulseModifier(durationMs, animation.loop)
            AnimationPreset.bounce -> bounceLoopModifier(durationMs, animation.loop)
            AnimationPreset.float -> floatModifier(durationMs, animation.loop)
            AnimationPreset.wiggle -> wiggleModifier(durationMs, animation.loop)
            AnimationPreset.rotate -> rotateModifier(durationMs, animation.loop)
            AnimationPreset.glow -> glowModifier(durationMs, animation.loop)
            AnimationPreset.shake -> shakeModifier(durationMs, animation.loop)
            AnimationPreset.heartbeat -> heartbeatModifier(durationMs, animation.loop)
            AnimationPreset.colorCycle -> colorCycleModifier(durationMs)
            AnimationPreset.swing -> swingModifier(durationMs, animation.loop)
            AnimationPreset.flash -> flashModifier(durationMs, animation.loop)
            AnimationPreset.motionPath, AnimationPreset.curvePath -> Modifier // Path animations handled by PathAnimationRenderer
        }
    }

    @Composable
    private fun fadeInModifier(durationMs: Int, delayMs: Int): Modifier {
        val alpha by animateOnce(0f, 1f, durationMs, delayMs)
        return Modifier.alpha(alpha)
    }

    @Composable
    private fun fadeSlideModifier(durationMs: Int, delayMs: Int, xOffset: Float = 0f, yOffset: Float = 0f): Modifier {
        val alpha by animateOnce(0f, 1f, durationMs, delayMs)
        val offsetX by animateOnce(xOffset, 0f, durationMs, delayMs)
        val offsetY by animateOnce(yOffset, 0f, durationMs, delayMs)
        return Modifier
            .alpha(alpha)
            .offset(x = offsetX.dp, y = offsetY.dp)
    }

    @Composable
    private fun zoomInModifier(durationMs: Int, delayMs: Int): Modifier {
        val scale by animateOnce(0f, 1f, durationMs, delayMs)
        val alpha by animateOnce(0f, 1f, durationMs, delayMs)
        return Modifier.scale(scale).alpha(alpha)
    }

    @Composable
    private fun bounceInModifier(durationMs: Int, delayMs: Int): Modifier {
        val scale by animateOnce(0.3f, 1f, durationMs, delayMs, easing = BounceEasing)
        val alpha by animateOnce(0f, 1f, (durationMs * 0.3).toInt(), delayMs)
        return Modifier.scale(scale).alpha(alpha)
    }

    @Composable
    private fun popInModifier(durationMs: Int, delayMs: Int): Modifier {
        val scale by animateOnce(0f, 1f, durationMs, delayMs, easing = OvershootEasing)
        return Modifier.scale(scale)
    }

    @Composable
    private fun fadeOutModifier(durationMs: Int, delayMs: Int): Modifier {
        val alpha by animateOnce(1f, 0f, durationMs, delayMs)
        return Modifier.alpha(alpha)
    }

    @Composable
    private fun slideOutModifier(durationMs: Int, delayMs: Int, yTarget: Float): Modifier {
        val alpha by animateOnce(1f, 0f, durationMs, delayMs)
        val offsetY by animateOnce(0f, yTarget, durationMs, delayMs)
        return Modifier.alpha(alpha).offset(y = offsetY.dp)
    }

    @Composable
    private fun zoomOutModifier(durationMs: Int, delayMs: Int): Modifier {
        val scale by animateOnce(1f, 0f, durationMs, delayMs)
        val alpha by animateOnce(1f, 0f, durationMs, delayMs)
        return Modifier.scale(scale).alpha(alpha)
    }

    @Composable
    private fun pulseModifier(durationMs: Int, loop: Boolean): Modifier {
        val scale by animateLoop(1f, 1.15f, durationMs, loop)
        return Modifier.scale(scale)
    }

    @Composable
    private fun bounceLoopModifier(durationMs: Int, loop: Boolean): Modifier {
        val offsetY by animateLoop(0f, -15f, durationMs, loop)
        return Modifier.offset(y = offsetY.dp)
    }

    @Composable
    private fun floatModifier(durationMs: Int, loop: Boolean): Modifier {
        val offsetY by animateLoop(0f, -10f, durationMs, loop)
        return Modifier.offset(y = offsetY.dp)
    }

    @Composable
    private fun wiggleModifier(durationMs: Int, loop: Boolean): Modifier {
        val rotation by animateLoop(-5f, 5f, durationMs, loop)
        return Modifier.graphicsLayer { rotationZ = rotation }
    }

    @Composable
    private fun rotateModifier(durationMs: Int, loop: Boolean): Modifier {
        val infiniteTransition = rememberInfiniteTransition(label = "rotate")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        return Modifier.graphicsLayer { rotationZ = rotation }
    }

    @Composable
    private fun shakeModifier(durationMs: Int, loop: Boolean): Modifier {
        val offsetX by animateLoop(-8f, 8f, (durationMs * 0.2).toInt(), loop)
        return Modifier.offset(x = offsetX.dp)
    }

    @Composable
    private fun heartbeatModifier(durationMs: Int, loop: Boolean): Modifier {
        val scale by animateLoop(1f, 1.3f, (durationMs * 0.5).toInt(), loop)
        return Modifier.scale(scale)
    }

    @Composable
    private fun swingModifier(durationMs: Int, loop: Boolean): Modifier {
        val rotation by animateLoop(-15f, 15f, durationMs, loop)
        return Modifier.graphicsLayer { rotationZ = rotation }
    }

    @Composable
    private fun flashModifier(durationMs: Int, loop: Boolean): Modifier {
        val alpha by animateLoop(1f, 0f, (durationMs * 0.5).toInt(), loop)
        return Modifier.alpha(alpha)
    }

    @Composable
    private fun glowModifier(durationMs: Int, loop: Boolean): Modifier {
        val radius by animateLoop(0f, 12f, durationMs, loop)
        val alpha by animateLoop(0f, 0.8f, durationMs, loop)
        return Modifier.graphicsLayer {
            shadowElevation = radius
            this.alpha = 1f // keep content visible
            ambientShadowColor = androidx.compose.ui.graphics.Color.White.copy(alpha = alpha)
        }
    }

    @Composable
    private fun colorCycleModifier(durationMs: Int): Modifier {
        val infiniteTransition = rememberInfiniteTransition(label = "colorCycle")
        val hue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "hue"
        )
        return Modifier.graphicsLayer {
            // Approximate hue rotation via color matrix overlay
            // The actual hue rotation is applied at the rendering level
            rotationZ = 0f // no-op; hue rotation handled by composable wrapper
        }
    }

    @Composable
    private fun typewriterModifier(text: String, durationMs: Int, delayMs: Int): Modifier {
        // Typewriter effect uses clip-based masking via alpha progression
        // The actual character reveal is handled by TypewriterText composable
        val progress by animateOnce(0f, 1f, durationMs, delayMs)
        return Modifier.graphicsLayer {
            clip = true
        }
    }

    @Composable
    private fun blurInModifier(durationMs: Int, delayMs: Int): Modifier {
        val blur by animateOnce(20f, 0f, durationMs, delayMs)
        val alpha by animateOnce(0f, 1f, durationMs, delayMs)
        return Modifier
            .alpha(alpha)
            .blur(blur.coerceAtLeast(0f).dp)
    }

    @Composable
    private fun blurOutModifier(durationMs: Int, delayMs: Int): Modifier {
        val blur by animateOnce(0f, 20f, durationMs, delayMs)
        val alpha by animateOnce(1f, 0f, durationMs, delayMs)
        return Modifier
            .alpha(alpha)
            .blur(blur.coerceAtLeast(0f).dp)
    }

    @Composable
    private fun flipInModifier(durationMs: Int, delayMs: Int, axisX: Boolean): Modifier {
        val rotation by animateOnce(90f, 0f, durationMs, delayMs)
        val alpha by animateOnce(0f, 1f, durationMs, delayMs)
        return Modifier
            .alpha(alpha)
            .graphicsLayer {
                if (axisX) rotationX = rotation else rotationY = rotation
                cameraDistance = 12f * density
            }
    }

    @Composable
    private fun shrinkOutModifier(durationMs: Int, delayMs: Int): Modifier {
        val scale by animateOnce(1f, 0f, durationMs, delayMs)
        val alpha by animateOnce(1f, 0f, durationMs, delayMs)
        return Modifier.scale(scale).alpha(alpha)
    }

    /** Exposed for TypewriterText composable to get the character reveal progress. */
    @Composable
    fun typewriterProgress(text: String, durationMs: Int, delayMs: Int): State<Float> {
        return animateOnce(0f, 1f, durationMs, delayMs)
    }

    // --- Helpers ---

    @Composable
    private fun animateOnce(
        from: Float, to: Float, durationMs: Int, delayMs: Int,
        easing: Easing = FastOutSlowInEasing
    ): State<Float> {
        val animatable = remember { Animatable(from) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(delayMs.toLong())
            animatable.animateTo(to, animationSpec = tween(durationMs, easing = easing))
        }
        return animatable.asState()
    }

    @Composable
    private fun animateLoop(from: Float, to: Float, durationMs: Int, loop: Boolean): State<Float> {
        val infiniteTransition = rememberInfiniteTransition(label = "loop")
        return infiniteTransition.animateFloat(
            initialValue = from,
            targetValue = to,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "value"
        )
    }
}

private val BounceEasing = Easing { fraction ->
    val n1 = 7.5625f; val d1 = 2.75f
    var t = fraction
    when {
        t < 1f / d1 -> n1 * t * t
        t < 2f / d1 -> { t -= 1.5f / d1; n1 * t * t + 0.75f }
        t < 2.5f / d1 -> { t -= 2.25f / d1; n1 * t * t + 0.9375f }
        else -> { t -= 2.625f / d1; n1 * t * t + 0.984375f }
    }
}

private val OvershootEasing = Easing { fraction ->
    val s = 1.70158f
    (fraction - 1f).let { t -> t * t * ((s + 1f) * t + s) + 1f }
}
