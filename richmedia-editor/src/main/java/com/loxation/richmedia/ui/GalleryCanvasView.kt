package com.loxation.richmedia.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loxation.richmedia.model.LayerPosition
import com.loxation.richmedia.model.MediaTransform
import com.loxation.richmedia.model.RichPostBlock
import kotlinx.coroutines.launch

/**
 * Internal editable gallery — swipeable pager of MediaCanvasViews.
 * Includes page counter with prev/next navigation buttons (iOS parity).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GalleryCanvasView(
    blocks: List<RichPostBlock>,
    selectedBlockId: String?,
    selectedLayerId: String?,
    isPlaying: Boolean,
    localImages: Map<String, Bitmap>,
    onBlockSelected: (String) -> Unit,
    onLayerSelected: (layerId: String, blockId: String) -> Unit,
    onLayerPositionChanged: (layerId: String, blockId: String, LayerPosition) -> Unit,
    onMediaTransformChanged: (MediaTransform, blockId: String) -> Unit,
    onTapCanvas: () -> Unit,
    onLayerLongPress: ((layerId: String, blockId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (blocks.isEmpty()) return

    val initialPage = blocks.indexOfFirst { it.id == selectedBlockId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { blocks.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        blocks.getOrNull(pagerState.currentPage)?.id?.let { onBlockSelected(it) }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gallery pager — aspect ratio naturally falls back to height-first
        // when the 9:16 canvas would exceed available vertical space
        Box(
            modifier = Modifier
                .aspectRatio(9f / 16f)
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val block = blocks[page]
                MediaCanvasView(
                    block = block,
                    isPlaying = isPlaying,
                    isEditing = true,
                    localImages = localImages,
                    selectedLayerId = if (block.id == selectedBlockId) selectedLayerId else null,
                    onLayerSelected = { layerId -> onLayerSelected(layerId, block.id) },
                    onLayerPositionChanged = { layerId, position ->
                        onLayerPositionChanged(layerId, block.id, position)
                    },
                    onMediaTransformChanged = { transform ->
                        onMediaTransformChanged(transform, block.id)
                    },
                    onTapCanvas = onTapCanvas,
                    onLayerLongPress = { layerId ->
                        onLayerLongPress?.invoke(layerId, block.id)
                    }
                )
            }
        }

        // Page indicator with prev/next buttons
        if (blocks.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = if (pagerState.currentPage > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Page counter
                Text(
                    "${pagerState.currentPage + 1} / ${blocks.size}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                // Next button
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < blocks.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage < blocks.size - 1
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = if (pagerState.currentPage < blocks.size - 1)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
