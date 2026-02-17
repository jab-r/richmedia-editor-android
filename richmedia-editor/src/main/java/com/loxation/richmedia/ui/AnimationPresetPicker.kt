package com.loxation.richmedia.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loxation.richmedia.model.AnimationCategory
import com.loxation.richmedia.model.AnimationPreset

/**
 * Visual gallery for selecting animation presets, organized by category.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationPresetPicker(
    selectedPreset: AnimationPreset?,
    onSelect: (AnimationPreset) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Animations",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            AnimationCategory.entries.forEach { category ->
                val presets = AnimationPreset.entries.filter { it.category == category }
                if (presets.isNotEmpty()) {
                    PresetSection(
                        category = category,
                        presets = presets,
                        selectedPreset = selectedPreset,
                        onSelect = onSelect
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetSection(
    category: AnimationCategory,
    presets: List<AnimationPreset>,
    selectedPreset: AnimationPreset?,
    onSelect: (AnimationPreset) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            category.displayName,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(presets) { preset ->
                PresetThumbnail(
                    preset = preset,
                    isSelected = selectedPreset == preset,
                    onTap = { onSelect(preset) }
                )
            }
        }
    }
}

@Composable
private fun PresetThumbnail(
    preset: AnimationPreset,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = preset.name)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onTap)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    else Modifier
                )
        ) {
            AnimatedPreviewText(preset = preset, infiniteTransition = infiniteTransition)
        }

        Spacer(Modifier.height(4.dp))

        Text(
            preset.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun AnimatedPreviewText(
    preset: AnimationPreset,
    infiniteTransition: InfiniteTransition
) {
    val baseModifier = Modifier

    val animatedModifier = when (preset) {
        AnimationPreset.fadeIn, AnimationPreset.blurIn -> {
            val alpha by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            Modifier.alpha(alpha)
        }
        AnimationPreset.fadeSlideUp -> {
            val alpha by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            val y by infiniteTransition.animateFloat(20f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "y")
            Modifier.alpha(alpha).offset(y = y.dp)
        }
        AnimationPreset.fadeSlideDown -> {
            val alpha by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            val y by infiniteTransition.animateFloat(-20f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "y")
            Modifier.alpha(alpha).offset(y = y.dp)
        }
        AnimationPreset.fadeSlideLeft -> {
            val alpha by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            val x by infiniteTransition.animateFloat(20f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "x")
            Modifier.alpha(alpha).offset(x = x.dp)
        }
        AnimationPreset.fadeSlideRight -> {
            val alpha by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            val x by infiniteTransition.animateFloat(-20f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "x")
            Modifier.alpha(alpha).offset(x = x.dp)
        }
        AnimationPreset.zoomIn -> {
            val s by infiniteTransition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "s")
            val alpha by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            Modifier.scale(s).alpha(alpha)
        }
        AnimationPreset.bounceIn -> {
            val s by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Restart), "s")
            Modifier.scale(s)
        }
        AnimationPreset.popIn -> {
            val s by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Restart), "s")
            val alpha by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Restart), "a")
            Modifier.scale(s).alpha(alpha)
        }
        AnimationPreset.fadeOut -> {
            val alpha by infiniteTransition.animateFloat(1f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            Modifier.alpha(alpha)
        }
        AnimationPreset.slideOutUp -> {
            val y by infiniteTransition.animateFloat(0f, -20f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "y")
            val alpha by infiniteTransition.animateFloat(1f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            Modifier.offset(y = y.dp).alpha(alpha)
        }
        AnimationPreset.slideOutDown -> {
            val y by infiniteTransition.animateFloat(0f, 20f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "y")
            val alpha by infiniteTransition.animateFloat(1f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            Modifier.offset(y = y.dp).alpha(alpha)
        }
        AnimationPreset.zoomOut -> {
            val s by infiniteTransition.animateFloat(1f, 0.5f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "s")
            val alpha by infiniteTransition.animateFloat(1f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            Modifier.scale(s).alpha(alpha)
        }
        AnimationPreset.shrinkOut -> {
            val s by infiniteTransition.animateFloat(1f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "s")
            Modifier.scale(s)
        }
        AnimationPreset.blurOut -> {
            val alpha by infiniteTransition.animateFloat(1f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "a")
            Modifier.alpha(alpha)
        }
        AnimationPreset.pulse -> {
            val s by infiniteTransition.animateFloat(1f, 1.1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), "s")
            Modifier.scale(s)
        }
        AnimationPreset.bounce -> {
            val y by infiniteTransition.animateFloat(0f, -10f, infiniteRepeatable(tween(600), RepeatMode.Reverse), "y")
            Modifier.offset(y = y.dp)
        }
        AnimationPreset.float -> {
            val y by infiniteTransition.animateFloat(-8f, 8f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), "y")
            Modifier.offset(y = y.dp)
        }
        AnimationPreset.wiggle -> {
            val r by infiniteTransition.animateFloat(-5f, 5f, infiniteRepeatable(tween(400), RepeatMode.Reverse), "r")
            Modifier.graphicsLayer { rotationZ = r }
        }
        AnimationPreset.rotate -> {
            val r by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), "r")
            Modifier.graphicsLayer { rotationZ = r }
        }
        AnimationPreset.glow -> {
            val a by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), "a")
            Modifier.graphicsLayer { shadowElevation = a * 12f }
        }
        AnimationPreset.shake -> {
            val x by infiniteTransition.animateFloat(-3f, 3f, infiniteRepeatable(tween(100), RepeatMode.Reverse), "x")
            Modifier.offset(x = x.dp)
        }
        AnimationPreset.heartbeat -> {
            val s by infiniteTransition.animateFloat(1f, 1.15f, infiniteRepeatable(tween(300), RepeatMode.Reverse), "s")
            Modifier.scale(s)
        }
        AnimationPreset.colorCycle -> {
            val hue by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), "h")
            Modifier.graphicsLayer { rotationZ = 0f } // placeholder
        }
        AnimationPreset.swing -> {
            val r by infiniteTransition.animateFloat(-8f, 8f, infiniteRepeatable(tween(600), RepeatMode.Reverse), "r")
            Modifier.graphicsLayer { rotationZ = r; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f) }
        }
        AnimationPreset.flash -> {
            val a by infiniteTransition.animateFloat(1f, 0f, infiniteRepeatable(tween(300), RepeatMode.Reverse), "a")
            Modifier.alpha(a)
        }
        AnimationPreset.flipInX -> {
            val r by infiniteTransition.animateFloat(90f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "r")
            Modifier.graphicsLayer { rotationX = r; cameraDistance = 12f * density }
        }
        AnimationPreset.flipInY -> {
            val r by infiniteTransition.animateFloat(90f, 0f, infiniteRepeatable(tween(1000), RepeatMode.Restart), "r")
            Modifier.graphicsLayer { rotationY = r; cameraDistance = 12f * density }
        }
        AnimationPreset.typewriter -> Modifier // static preview for typewriter
        AnimationPreset.motionPath, AnimationPreset.curvePath -> Modifier
    }

    Text(
        "Aa",
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        modifier = animatedModifier
    )
}
