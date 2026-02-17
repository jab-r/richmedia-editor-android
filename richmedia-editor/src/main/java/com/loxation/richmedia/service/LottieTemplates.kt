package com.loxation.richmedia.service

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.loxation.richmedia.model.LottieAnimation

data class LottieTemplate(
    val name: String,
    val category: String,
    val icon: ImageVector,
    val filename: String
)

object LottieTemplates {

    val allTemplates = listOf(
        LottieTemplate("Confetti", "Celebration", Icons.Default.Celebration, "confetti"),
        LottieTemplate("Sparkles", "Effects", Icons.Default.AutoAwesome, "sparkles"),
        LottieTemplate("Loading", "Utility", Icons.Default.Refresh, "loading"),
        LottieTemplate("Heart Beat", "Emotion", Icons.Default.Favorite, "heart_beat"),
        LottieTemplate("Star Burst", "Effects", Icons.Default.Star, "star_burst"),
        LottieTemplate("Checkmark", "Utility", Icons.Default.CheckCircle, "checkmark")
    )

    fun loadTemplate(template: LottieTemplate): LottieAnimation {
        val json = generatePlaceholderJson(template.name)
        return LottieAnimation(
            jsonData = json,
            name = template.name,
            duration = 2.0,
            frameRate = 30.0,
            loops = true
        )
    }

    private fun generatePlaceholderJson(name: String): String {
        return """
        {
            "v": "5.7.4",
            "fr": 30,
            "ip": 0,
            "op": 60,
            "w": 512,
            "h": 512,
            "nm": "$name",
            "layers": []
        }
        """.trimIndent()
    }
}
