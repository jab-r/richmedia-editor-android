package com.loxation.richmedia.ui

import android.graphics.Bitmap
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loxation.richmedia.model.MediaInput
import com.loxation.richmedia.model.RichPostContent
import com.loxation.richmedia.viewmodel.AnimatedPostEditorViewModel

/**
 * Main public entry point — the richmedia editor screen.
 *
 * Usage:
 * ```
 * AnimatedPostEditorScreen(
 *     media = listOf(MediaInput.Image(bitmap)),
 *     onComplete = { content, images, location -> /* submit */ },
 *     onCancel = { /* dismiss */ }
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedPostEditorScreen(
    media: List<MediaInput> = emptyList(),
    initialText: String? = null,
    existingContent: RichPostContent? = null,
    existingLocalImages: Map<String, Bitmap> = emptyMap(),
    onComplete: (RichPostContent, Map<String, Bitmap>, Location?) -> Unit,
    onCancel: () -> Unit,
    viewModel: AnimatedPostEditorViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        if (existingContent != null) {
            viewModel.loadContent(existingContent, existingLocalImages)
        } else if (media.isNotEmpty()) {
            viewModel.loadMedia(media, initialText)
        }
    }

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onComplete(
                                viewModel.richContent,
                                viewModel.localImages.toMap(),
                                viewModel.firstMediaLocation
                            )
                        },
                        enabled = state.blocks.isNotEmpty()
                    ) {
                        Text("Done")
                    }
                }
            )
        },
        bottomBar = {
            EditorBottomToolbar(
                canAddLayer = state.canAddLayer,
                isPlaying = state.isPlaying,
                onAddText = {
                    state.selectedBlockId?.let { viewModel.addTextLayer(it) }
                },
                onTogglePlay = { viewModel.togglePlayback() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (state.blocks.isEmpty()) {
                Text(
                    "Add media to get started",
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                // TODO: MediaCanvasView with block pager, text layer overlays, gesture handling
                GalleryCanvasView(
                    blocks = state.blocks,
                    selectedBlockId = state.selectedBlockId,
                    selectedLayerId = state.selectedLayerId,
                    isPlaying = state.isPlaying,
                    localImages = viewModel.localImages,
                    onBlockSelected = { viewModel.selectBlock(it) },
                    onLayerSelected = { layerId, blockId -> viewModel.selectLayer(layerId, blockId) },
                    onLayerPositionChanged = { layerId, blockId, position ->
                        viewModel.updateLayer(layerId, blockId) { it.copy(position = position) }
                    },
                    onMediaTransformChanged = { transform, blockId ->
                        viewModel.updateMediaTransform(transform, blockId)
                    },
                    onTapCanvas = { viewModel.deselectAll() }
                )
            }
        }
    }
}

@Composable
private fun EditorBottomToolbar(
    canAddLayer: Boolean,
    isPlaying: Boolean,
    onAddText: () -> Unit,
    onTogglePlay: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAddText, enabled = canAddLayer) {
                Icon(Icons.Default.Add, contentDescription = "Add text")
            }
            // TODO: Lottie picker button
            // TODO: Animation picker button
            IconButton(onClick = onTogglePlay) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}
