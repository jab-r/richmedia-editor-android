package com.loxation.richmedia.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.loxation.richmedia.model.LayerPosition
import com.loxation.richmedia.model.MediaTransform
import com.loxation.richmedia.model.RichPostBlock

/**
 * Single block canvas: background media + text layer overlays.
 * Handles pinch-to-zoom/pan on background media, tap-to-deselect, and layer overlays.
 */
@Composable
internal fun MediaCanvasView(
    block: RichPostBlock,
    isPlaying: Boolean,
    isEditing: Boolean,
    localImages: Map<String, Bitmap>,
    selectedLayerId: String?,
    onLayerSelected: (layerId: String) -> Unit,
    onLayerTapped: ((layerId: String) -> Unit)? = null,
    onLayerPositionChanged: (layerId: String, LayerPosition) -> Unit,
    onMediaTransformChanged: (MediaTransform) -> Unit,
    onTapCanvas: () -> Unit,
    onLayerLongPress: ((layerId: String) -> Unit)? = null
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val transform = block.mediaTransform ?: MediaTransform()

    // Media transform gesture state
    var mediaScale by remember(transform.scale) { mutableFloatStateOf(transform.scale) }
    var mediaOffsetX by remember(transform.offsetX) { mutableFloatStateOf(transform.offsetX) }
    var mediaOffsetY by remember(transform.offsetY) { mutableFloatStateOf(transform.offsetY) }

    val mediaTransformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (mediaScale * zoomChange).coerceIn(1f, 5f)
        val maxOffsetX = canvasSize.width * (newScale - 1) / 2
        val maxOffsetY = canvasSize.height * (newScale - 1) / 2
        mediaScale = newScale
        mediaOffsetX = (mediaOffsetX + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
        mediaOffsetY = (mediaOffsetY + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
    }

    // Commit media transform when gestures end
    LaunchedEffect(mediaScale, mediaOffsetX, mediaOffsetY) {
        if (mediaScale != transform.scale || mediaOffsetX != transform.offsetX || mediaOffsetY != transform.offsetY) {
            onMediaTransformChanged(MediaTransform(mediaScale, mediaOffsetX, mediaOffsetY))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .onSizeChanged { canvasSize = it }
    ) {
        // Background media with transform
        val localBitmap = localImages[block.id]
        val hasMedia = localBitmap != null || block.url != null

        if (localBitmap != null) {
            Image(
                bitmap = localBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = mediaScale
                        scaleY = mediaScale
                        translationX = mediaOffsetX
                        translationY = mediaOffsetY
                    }
            )
        } else if (block.video != null && block.url != null) {
            VideoBackground(
                url = block.url,
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = mediaScale
                        scaleY = mediaScale
                        translationX = mediaOffsetX
                        translationY = mediaOffsetY
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
                        scaleX = mediaScale
                        scaleY = mediaScale
                        translationX = mediaOffsetX
                        translationY = mediaOffsetY
                    }
            )
        } else {
            // Placeholder
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_gallery),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        // Media gesture layer (tap to deselect + pinch/pan)
        if (isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { onTapCanvas() }
                    }
                    .transformable(state = mediaTransformState)
            )
        }

        // Lottie overlay
        block.lottieOverlay?.let { lottie ->
            LottieOverlay(jsonData = lottie.jsonData, play = isPlaying, loop = lottie.loops)
        }

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
                    onTapped = { onLayerTapped?.invoke(layer.id) },
                    onPositionChanged = { position -> onLayerPositionChanged(layer.id, position) },
                    onLongPress = { onLayerLongPress?.invoke(layer.id) }
                )
            }
    }
}

@Composable
private fun VideoBackground(url: String, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = false
            volume = 0f // muted during editing
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) player.play() else player.pause()
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun LottieOverlay(jsonData: String, play: Boolean, loop: Boolean) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(jsonData)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = play,
        iterations = if (loop) LottieConstants.IterateForever else 1
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.fillMaxSize()
    )
}
