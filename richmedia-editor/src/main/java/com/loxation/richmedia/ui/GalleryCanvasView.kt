package com.loxation.richmedia.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.loxation.richmedia.model.LayerPosition
import com.loxation.richmedia.model.MediaTransform
import com.loxation.richmedia.model.RichPostBlock

/**
 * Internal editable gallery — swipeable pager of MediaCanvasViews.
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
    onTapCanvas: () -> Unit
) {
    if (blocks.isEmpty()) return

    val initialPage = blocks.indexOfFirst { it.id == selectedBlockId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { blocks.size })

    LaunchedEffect(pagerState.currentPage) {
        blocks.getOrNull(pagerState.currentPage)?.id?.let { onBlockSelected(it) }
    }

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
                onTapCanvas = onTapCanvas
            )
        }

        // Page dots
        if (blocks.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(blocks.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}
