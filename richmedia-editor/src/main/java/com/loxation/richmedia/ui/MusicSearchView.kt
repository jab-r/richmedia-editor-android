package com.loxation.richmedia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.loxation.richmedia.model.MusicTrack
import com.loxation.richmedia.service.MusicSearchService
import com.loxation.richmedia.service.PreviewAudioPlayer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun MusicSearchView(
    currentTrack: MusicTrack?,
    audioPlayer: PreviewAudioPlayer,
    onSelect: (MusicTrack?) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val previewPlaying by audioPlayer.isPlaying.collectAsState()
    val previewTrack by audioPlayer.currentTrack.collectAsState()

    // Debounced search
    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .debounce(400)
            .collect { q ->
                if (q.isBlank()) {
                    results = emptyList()
                    isLoading = false
                    return@collect
                }
                isLoading = true
                results = MusicSearchService.search(q)
                isLoading = false
            }
    }

    ModalBottomSheet(
        onDismissRequest = {
            audioPlayer.stop()
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                "Add Music",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search songs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Results
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp)
                        )
                    }
                    results.isEmpty() && query.isNotBlank() && !isLoading -> {
                        Text(
                            "No results found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp)
                        )
                    }
                    else -> {
                        LazyColumn {
                            items(results, key = { it.appleMusicID }) { track ->
                                MusicResultRow(
                                    track = track,
                                    isPreviewPlaying = previewPlaying && previewTrack?.appleMusicID == track.appleMusicID,
                                    onTap = {
                                        audioPlayer.stop()
                                        onSelect(track)
                                        onDismiss()
                                    },
                                    onTogglePreview = {
                                        if (previewPlaying && previewTrack?.appleMusicID == track.appleMusicID) {
                                            audioPlayer.pause()
                                        } else {
                                            audioPlayer.play(track)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Current selection bar
            if (currentTrack != null) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${currentTrack.trackName} — ${currentTrack.artistName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                audioPlayer.stop()
                                onSelect(null)
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove track",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicResultRow(
    track: MusicTrack,
    isPreviewPlaying: Boolean,
    onTap: () -> Unit,
    onTogglePreview: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album artwork
        if (track.artworkURL != null) {
            AsyncImage(
                model = track.artworkURL,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Track info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.trackName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (track.albumName != null) {
                Text(
                    track.albumName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Preview play/pause
        IconButton(onClick = onTogglePreview) {
            Icon(
                if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPreviewPlaying) "Pause preview" else "Play preview",
                tint = Color(0xFFE91E63)
            )
        }
    }
}
