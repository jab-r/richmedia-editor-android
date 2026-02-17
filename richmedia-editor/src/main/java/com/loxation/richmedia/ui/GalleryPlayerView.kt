package com.loxation.richmedia.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.loxation.richmedia.model.*
import com.loxation.richmedia.service.AnimationRenderer
import com.loxation.richmedia.util.fromHex

/**
 * Read-only TikTok-style gallery player for displaying animated posts.
 * Features: play/pause, animated text layers, Lottie overlays, media transforms,
 * gradient caption, capsule page indicator.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryPlayerView(
    content: RichPostContent,
    localImages: Map<String, Bitmap> = emptyMap(),
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val blocks = content.blocks
    if (blocks.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { blocks.size })
    var isPlaying by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            PlayerPageView(
                block = blocks[page],
                isPlaying = isPlaying,
                localImage = localImages[blocks[page].id]
            )
        }

        // Overlay controls
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.shadow(2.dp, CircleShape)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Capsule page indicator
                if (blocks.size > 1) {
                    Row(
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(blocks.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                    .width(if (isSelected) 24.dp else 8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isSelected) Color.White
                                        else Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Play/pause button
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.shadow(2.dp, CircleShape)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom caption with gradient
            blocks.getOrNull(pagerState.currentPage)?.caption?.let { caption ->
                if (caption.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.shadow(2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerPageView(
    block: RichPostBlock,
    isPlaying: Boolean,
    localImage: Bitmap?
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val transform = block.mediaTransform ?: MediaTransform()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
    ) {
        // Background media with transform
        val mediaModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = transform.scale
                scaleY = transform.scale
                translationX = transform.offsetX
                translationY = transform.offsetY
            }

        if (localImage != null) {
            androidx.compose.foundation.Image(
                bitmap = localImage.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = mediaModifier
            )
        } else if (block.video != null && block.url != null) {
            PlayerVideoView(url = block.url, isPlaying = isPlaying, modifier = mediaModifier)
        } else if (block.url != null) {
            AsyncImage(
                model = block.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = mediaModifier
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }

        // Lottie overlay
        block.lottieOverlay?.let { lottie ->
            val composition by rememberLottieComposition(
                LottieCompositionSpec.JsonString(lottie.jsonData)
            )
            val progress by animateLottieCompositionAsState(
                composition = composition,
                isPlaying = isPlaying,
                iterations = if (lottie.loops) LottieConstants.IterateForever else 1
            )
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Animated text layers
        block.textLayers
            ?.filter { it.visible }
            ?.sortedBy { it.zIndex }
            ?.forEach { layer ->
                PlayerTextLayer(
                    layer = layer,
                    canvasSize = canvasSize,
                    isPlaying = isPlaying
                )
            }
    }
}

@Composable
private fun PlayerTextLayer(
    layer: TextLayer,
    canvasSize: IntSize,
    isPlaying: Boolean
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pos = layer.position
    val style = layer.style

    val offsetXPx = pos.x * canvasSize.width
    val offsetYPx = pos.y * canvasSize.height

    val textColor = runCatching { fromHex(style.color) }.getOrDefault(Color.White)
    val fontFamily = when (style.font) {
        "Georgia" -> FontFamily.Serif
        "Helvetica" -> FontFamily.SansSerif
        "Courier" -> FontFamily.Monospace
        "Times New Roman" -> FontFamily.Serif
        else -> FontFamily.Default
    }
    val shadow = style.shadow?.let { s ->
        val sc = runCatching { fromHex(s.color) }.getOrDefault(Color.Black)
        androidx.compose.ui.graphics.Shadow(
            color = sc.copy(alpha = s.opacity),
            offset = androidx.compose.ui.geometry.Offset(s.offset.width, s.offset.height),
            blurRadius = s.radius
        )
    }
    val textDecoration = buildList {
        if (style.underline) add(TextDecoration.Underline)
        if (style.strikethrough) add(TextDecoration.LineThrough)
    }.let { if (it.isEmpty()) null else TextDecoration.combine(it) }

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
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
            }
            .then(animModifier)
    ) {
        Text(
            text = layer.text,
            style = androidx.compose.ui.text.TextStyle(
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

@Composable
private fun PlayerVideoView(url: String, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = isPlaying
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
