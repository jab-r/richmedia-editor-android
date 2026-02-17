package com.loxation.richmedia.ui

import android.graphics.Bitmap
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loxation.richmedia.model.MediaInput
import com.loxation.richmedia.model.RichPostContent
import com.loxation.richmedia.model.TextLayer
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
    onRequestMedia: (() -> Unit)? = null,
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
    var showTextEditor by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var editingLayerId by remember { mutableStateOf<String?>(null) }
    var floatingText by remember { mutableStateOf("") }

    // Clear editing state when playback starts
    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) {
            editingLayerId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Animated Post", fontWeight = FontWeight.SemiBold) },
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
            if (state.blocks.isNotEmpty()) {
                EditorBottomToolbar(
                    isPlaying = state.isPlaying,
                    hasBlocks = state.blocks.isNotEmpty(),
                    onTogglePlay = { viewModel.togglePlayback() },
                    onAddMedia = { onRequestMedia?.invoke() },
                    onAddText = {
                        val blockId = state.selectedBlockId ?: state.blocks.firstOrNull()?.id
                        if (blockId != null) {
                            viewModel.addTextLayer(blockId)
                            val newLayerId = viewModel.state.value.selectedLayerId
                            if (newLayerId != null) {
                                editingLayerId = newLayerId
                                floatingText = "Text"
                            }
                        }
                    },
                    onHelp = { showHelp = true }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.blocks.isEmpty()) {
                EmptyStateView(onAddMedia = { onRequestMedia?.invoke() })
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GalleryCanvasView(
                        blocks = state.blocks,
                        selectedBlockId = state.selectedBlockId,
                        selectedLayerId = state.selectedLayerId,
                        isPlaying = state.isPlaying,
                        localImages = viewModel.localImages,
                        onBlockSelected = { viewModel.selectBlock(it) },
                        onLayerSelected = { layerId, blockId ->
                            viewModel.selectLayer(layerId, blockId)
                            // Single tap: enter inline text editing
                            editingLayerId = layerId
                            val block = state.blocks.find { it.id == blockId }
                            val layer = block?.textLayers?.find { it.id == layerId }
                            floatingText = layer?.text ?: ""
                        },
                        onLayerPositionChanged = { layerId, blockId, position ->
                            viewModel.updateLayer(layerId, blockId) { it.copy(position = position) }
                        },
                        onMediaTransformChanged = { transform, blockId ->
                            viewModel.updateMediaTransform(transform, blockId)
                        },
                        onTapCanvas = {
                            if (editingLayerId != null) {
                                finishEditing(
                                    editingLayerId, floatingText,
                                    state.selectedBlockId, viewModel
                                )
                                editingLayerId = null
                            } else {
                                viewModel.deselectAll()
                            }
                        },
                        onLayerLongPress = { layerId, blockId ->
                            // Long press: open style/animation editor
                            editingLayerId = null
                            viewModel.selectLayer(layerId, blockId)
                            showTextEditor = true
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Floating text input above keyboard
            if (editingLayerId != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    FloatingTextInput(
                        text = floatingText,
                        onTextChange = { newText ->
                            floatingText = newText
                            val blockId = state.selectedBlockId ?: return@FloatingTextInput
                            val layerId = editingLayerId ?: return@FloatingTextInput
                            viewModel.updateLayer(layerId, blockId) { it.copy(text = newText) }
                        },
                        onDone = {
                            finishEditing(
                                editingLayerId, floatingText,
                                state.selectedBlockId, viewModel
                            )
                            editingLayerId = null
                        }
                    )
                }
            }
        }
    }

    // Text layer editor sheet (long press)
    if (showTextEditor) {
        val blockId = state.selectedBlockId
        val layerId = state.selectedLayerId
        val block = state.blocks.find { it.id == blockId }
        val layer = block?.textLayers?.find { it.id == layerId }
        if (layer != null && blockId != null && layerId != null) {
            TextLayerEditorSheet(
                layer = layer,
                onSave = { updatedLayer ->
                    viewModel.updateLayer(layerId, blockId) {
                        it.copy(
                            text = updatedLayer.text,
                            style = updatedLayer.style,
                            animation = updatedLayer.animation
                        )
                    }
                    showTextEditor = false
                },
                onDismiss = { showTextEditor = false }
            )
        }
    }

    // Help sheet
    if (showHelp) {
        HelpSheet(onDismiss = { showHelp = false })
    }
}

private fun finishEditing(
    editingLayerId: String?,
    floatingText: String,
    selectedBlockId: String?,
    viewModel: AnimatedPostEditorViewModel
) {
    val trimmed = floatingText.trim()
    if (trimmed.isEmpty() && editingLayerId != null && selectedBlockId != null) {
        viewModel.deleteLayer(editingLayerId, selectedBlockId)
    }
}

@Composable
private fun EmptyStateView(onAddMedia: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Icon circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(60.dp)
                )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Create Animated Post",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Add photos or videos with animated text overlays",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onAddMedia,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Media", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FloatingTextInput(
    text: String,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Enter text") },
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 3
            )
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onDone) {
                Text("Done", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EditorBottomToolbar(
    isPlaying: Boolean,
    hasBlocks: Boolean,
    onTogglePlay: () -> Unit,
    onAddMedia: () -> Unit,
    onAddText: () -> Unit,
    onHelp: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause
            ToolbarButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                label = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.primary,
                onClick = onTogglePlay
            )

            // Add Media
            ToolbarButton(
                icon = Icons.Default.Add,
                label = "Media",
                tint = MaterialTheme.colorScheme.primary,
                onClick = onAddMedia
            )

            // Add Text
            ToolbarButton(
                icon = Icons.Default.TextFields,
                label = "Text",
                tint = Color(0xFF4CAF50),
                enabled = hasBlocks,
                onClick = onAddText
            )

            // Help
            ToolbarButton(
                icon = Icons.Default.Info,
                label = "Help",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                onClick = onHelp
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (enabled) tint else tint.copy(alpha = 0.3f)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Help",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            HelpSection(
                icon = Icons.Default.TouchApp,
                title = "Gestures",
                tips = listOf(
                    "Tap text to edit inline",
                    "Long press text for style & animation",
                    "Drag text layers to reposition",
                    "Pinch to scale text size",
                    "Rotate with two fingers",
                    "Tap empty canvas to deselect"
                )
            )

            Spacer(Modifier.height(12.dp))

            HelpSection(
                icon = Icons.Default.AutoAwesome,
                title = "Animations",
                tips = listOf(
                    "Long press a layer to open style & animation",
                    "Choose from 15+ presets",
                    "Tap Play to preview animations",
                    "Draw custom motion paths"
                )
            )

            Spacer(Modifier.height(12.dp))

            HelpSection(
                icon = Icons.Default.Palette,
                title = "Styling",
                tips = listOf(
                    "Long press a layer to open style editor",
                    "Font, color, shadow, outline",
                    "Bold, italic, underline"
                )
            )

            Spacer(Modifier.height(12.dp))

            HelpSection(
                icon = Icons.Default.Videocam,
                title = "Video Support",
                tips = listOf(
                    "Add text to video backgrounds",
                    "Sync animations with playback",
                    "Muted during editing",
                    "Full playback on export"
                )
            )
        }
    }
}

@Composable
private fun HelpSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tips: List<String>
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(start = 40.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("\u2022", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
