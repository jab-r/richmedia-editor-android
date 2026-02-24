package com.loxation.richmedia.service

import com.loxation.richmedia.model.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Serializable
private data class ITunesResponse(
    val resultCount: Int = 0,
    val results: List<ITunesResult> = emptyList()
)

@Serializable
private data class ITunesResult(
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val previewUrl: String? = null,
    val artworkUrl100: String? = null,
    val kind: String? = null
)

private val json = Json { ignoreUnknownKeys = true }

object MusicSearchService {

    suspend fun search(query: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL("https://itunes.apple.com/search?term=$encoded&media=music&limit=20")

        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"

            if (connection.responseCode != 200) return@withContext emptyList()

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val response = json.decodeFromString<ITunesResponse>(body)

            response.results
                .filter { it.kind == "song" && it.previewUrl != null && it.trackId != null }
                .mapNotNull { result ->
                    MusicTrack(
                        trackName = result.trackName ?: return@mapNotNull null,
                        artistName = result.artistName ?: return@mapNotNull null,
                        albumName = result.collectionName,
                        previewURL = result.previewUrl!!,
                        artworkURL = result.artworkUrl100,
                        appleMusicID = result.trackId!!.toString()
                    )
                }
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
