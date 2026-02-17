package com.loxation.richmedia.model

import android.graphics.Bitmap
import android.net.Uri

sealed class MediaInput {
    abstract val mediaId: String?

    data class Image(
        val bitmap: Bitmap,
        val url: String? = null,
        override val mediaId: String? = null
    ) : MediaInput()

    data class Video(
        val uri: Uri,
        override val mediaId: String? = null
    ) : MediaInput()

    val isVideo: Boolean get() = this is Video
    val isImage: Boolean get() = this is Image
}
