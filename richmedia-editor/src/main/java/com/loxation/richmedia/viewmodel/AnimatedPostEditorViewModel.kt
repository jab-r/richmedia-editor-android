package com.loxation.richmedia.viewmodel

import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.loxation.richmedia.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EditorState(
    val blocks: List<RichPostBlock> = emptyList(),
    val selectedBlockId: String? = null,
    val selectedLayerId: String? = null,
    val isPlaying: Boolean = false,
    val editingLayerId: String? = null,
    val showingStyleEditor: Boolean = false,
    val showingAnimationPicker: Boolean = false,
    val musicTrack: MusicTrack? = null
) {
    val selectedBlock: RichPostBlock?
        get() = blocks.find { it.id == selectedBlockId }

    val selectedLayer: TextLayer?
        get() = selectedBlock?.textLayers?.find { it.id == selectedLayerId }

    val canAddLayer: Boolean
        get() = (selectedBlock?.textLayers?.size ?: 0) < MAX_LAYERS_PER_BLOCK
}

private const val MAX_LAYERS_PER_BLOCK = 10

class AnimatedPostEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    /** Local bitmaps keyed by block ID, not yet uploaded. */
    val localImages = mutableMapOf<String, Bitmap>()

    /** EXIF GPS location from the first image/video. */
    var firstMediaLocation: Location? = null
        private set

    val richContent: RichPostContent
        get() = RichPostContent(
            version = 1,
            blocks = _state.value.blocks,
            musicTrack = _state.value.musicTrack
        )

    // --- Block management ---

    fun addImageBlock(bitmap: Bitmap, url: String? = null, mediaId: String? = null) {
        val blockId = java.util.UUID.randomUUID().toString()
        localImages[blockId] = bitmap
        val block = RichPostBlock(id = blockId, image = mediaId ?: blockId, url = url)
        _state.update { it.copy(blocks = it.blocks + block, selectedBlockId = blockId) }
    }

    fun addVideoBlock(uri: Uri, mediaId: String? = null) {
        val blockId = java.util.UUID.randomUUID().toString()
        val block = RichPostBlock(id = blockId, video = mediaId ?: blockId, url = uri.toString())
        _state.update { it.copy(blocks = it.blocks + block, selectedBlockId = blockId) }
    }

    fun deleteBlock(blockId: String) {
        localImages.remove(blockId)
        _state.update { state ->
            val newBlocks = state.blocks.filter { it.id != blockId }
            state.copy(
                blocks = newBlocks,
                selectedBlockId = if (state.selectedBlockId == blockId) newBlocks.firstOrNull()?.id else state.selectedBlockId,
                selectedLayerId = if (state.selectedBlockId == blockId) null else state.selectedLayerId
            )
        }
    }

    fun selectBlock(blockId: String) {
        _state.update { it.copy(selectedBlockId = blockId, selectedLayerId = null) }
    }

    // --- Layer management ---

    fun addTextLayer(blockId: String, text: String = "Text") {
        _state.update { state ->
            val blockIndex = state.blocks.indexOfFirst { it.id == blockId }
            if (blockIndex < 0) return@update state
            val block = state.blocks[blockIndex]
            val layers = block.textLayers.orEmpty()
            if (layers.size >= MAX_LAYERS_PER_BLOCK) return@update state

            val yOffset = 0.3f + (layers.size * 0.06f).coerceAtMost(0.4f)
            val layer = TextLayer(
                text = text,
                position = LayerPosition(x = 0.5f, y = yOffset),
                zIndex = layers.size
            )
            val updatedBlock = block.copy(textLayers = layers + layer)
            val newBlocks = state.blocks.toMutableList().also { it[blockIndex] = updatedBlock }
            state.copy(blocks = newBlocks, selectedLayerId = layer.id)
        }
    }

    fun deleteLayer(layerId: String, blockId: String) {
        _state.update { state ->
            val blockIndex = state.blocks.indexOfFirst { it.id == blockId }
            if (blockIndex < 0) return@update state
            val block = state.blocks[blockIndex]
            val updatedBlock = block.copy(textLayers = block.textLayers?.filter { it.id != layerId })
            val newBlocks = state.blocks.toMutableList().also { it[blockIndex] = updatedBlock }
            state.copy(
                blocks = newBlocks,
                selectedLayerId = if (state.selectedLayerId == layerId) null else state.selectedLayerId
            )
        }
    }

    fun updateLayer(layerId: String, blockId: String, transform: (TextLayer) -> TextLayer) {
        _state.update { state ->
            val blockIndex = state.blocks.indexOfFirst { it.id == blockId }
            if (blockIndex < 0) return@update state
            val block = state.blocks[blockIndex]
            val updatedLayers = block.textLayers?.map {
                if (it.id == layerId) transform(it) else it
            }
            val updatedBlock = block.copy(textLayers = updatedLayers)
            val newBlocks = state.blocks.toMutableList().also { it[blockIndex] = updatedBlock }
            state.copy(blocks = newBlocks)
        }
    }

    fun selectLayer(layerId: String, blockId: String) {
        _state.update { it.copy(selectedBlockId = blockId, selectedLayerId = layerId) }
    }

    fun deselectAll() {
        _state.update { it.copy(selectedLayerId = null) }
    }

    // --- Media transform ---

    fun updateMediaTransform(transform: MediaTransform, blockId: String) {
        _state.update { state ->
            val blockIndex = state.blocks.indexOfFirst { it.id == blockId }
            if (blockIndex < 0) return@update state
            val updatedBlock = state.blocks[blockIndex].copy(mediaTransform = transform)
            val newBlocks = state.blocks.toMutableList().also { it[blockIndex] = updatedBlock }
            state.copy(blocks = newBlocks)
        }
    }

    // --- Lottie overlay ---

    fun setLottieOverlay(animation: LottieAnimation?, blockId: String) {
        _state.update { state ->
            val blockIndex = state.blocks.indexOfFirst { it.id == blockId }
            if (blockIndex < 0) return@update state
            val updatedBlock = state.blocks[blockIndex].copy(lottieOverlay = animation)
            val newBlocks = state.blocks.toMutableList().also { it[blockIndex] = updatedBlock }
            state.copy(blocks = newBlocks)
        }
    }

    // --- Playback ---

    fun togglePlayback() {
        _state.update { it.copy(isPlaying = !it.isPlaying) }
    }

    // --- Load existing content ---

    fun setMusicTrack(track: MusicTrack?) {
        _state.update { it.copy(musicTrack = track) }
    }

    fun loadContent(content: RichPostContent, images: Map<String, Bitmap> = emptyMap()) {
        localImages.clear()
        localImages.putAll(images)
        _state.update {
            EditorState(
                blocks = content.blocks,
                selectedBlockId = content.blocks.firstOrNull()?.id,
                musicTrack = content.musicTrack
            )
        }
    }

    fun loadMedia(items: List<MediaInput>, initialText: String? = null) {
        items.forEach { input ->
            when (input) {
                is MediaInput.Image -> addImageBlock(input.bitmap, input.url, input.mediaId)
                is MediaInput.Video -> addVideoBlock(input.uri, input.mediaId)
            }
        }
        if (initialText != null) {
            _state.value.blocks.firstOrNull()?.id?.let { addTextLayer(it, initialText) }
        }
    }
}
