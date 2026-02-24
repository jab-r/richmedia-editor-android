package com.loxation.richmedia.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    prettyPrint = false
}

@Serializable
data class RichPostContent(
    val version: Int = 1,
    val blocks: List<RichPostBlock> = emptyList(),
    val musicTrack: MusicTrack? = null
) {
    fun toJsonString(): String? = runCatching { json.encodeToString(this) }.getOrNull()

    companion object {
        const val CONTENT_TYPE = "application/vnd.loxation.richmedia+json"

        fun fromJsonString(jsonString: String): RichPostContent? =
            runCatching { json.decodeFromString<RichPostContent>(jsonString) }.getOrNull()
    }
}
