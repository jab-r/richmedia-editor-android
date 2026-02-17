package com.loxation.richmedia.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Parse a hex color string (#RGB, #RRGGBB, or #AARRGGBB) to Compose Color. */
fun fromHex(hex: String): Color {
    val stripped = hex.removePrefix("#")
    val argb = when (stripped.length) {
        3 -> {
            val r = stripped[0].toString().repeat(2)
            val g = stripped[1].toString().repeat(2)
            val b = stripped[2].toString().repeat(2)
            "FF$r$g$b"
        }
        6 -> "FF$stripped"
        8 -> stripped
        else -> "FFFFFFFF"
    }
    return Color(argb.toLong(16).toInt())
}

/** Convert a Compose Color to #RRGGBB hex string. */
fun Color.toHex(): String {
    val argb = this.toArgb()
    return String.format("#%06X", argb and 0xFFFFFF)
}
