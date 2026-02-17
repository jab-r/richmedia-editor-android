package com.loxation.richmedia.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.loxation.richmedia.model.RichPostContent

/**
 * Read-only TikTok-style gallery player for displaying animated posts.
 * Shows blocks in a horizontal pager with page indicator dots.
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

    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            MediaCanvasView(
                block = blocks[page],
                isPlaying = true,
                isEditing = false,
                localImages = localImages,
                selectedLayerId = null,
                onLayerSelected = {},
                onLayerPositionChanged = { _, _ -> },
                onMediaTransformChanged = {},
                onTapCanvas = {}
            )
        }

        // Page indicator
        if (blocks.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
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

        // Close button
        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        // Caption
        blocks.getOrNull(pagerState.currentPage)?.caption?.let { caption ->
            Text(
                text = caption,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}
