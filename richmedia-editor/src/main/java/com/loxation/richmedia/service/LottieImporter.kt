package com.loxation.richmedia.service

import com.loxation.richmedia.model.LottieAnimation
import org.json.JSONObject

object LottieImporter {

    fun importAnimation(data: ByteArray, name: String): LottieAnimation? {
        return runCatching {
            val jsonString = String(data, Charsets.UTF_8)
            if (!validateLottieJson(jsonString)) return null

            val json = JSONObject(jsonString)
            val frameRate = json.optDouble("fr", 60.0)
            val inPoint = json.optDouble("ip", 0.0)
            val outPoint = json.optDouble("op", 0.0)
            val duration = if (frameRate > 0) (outPoint - inPoint) / frameRate else 0.0

            LottieAnimation(
                jsonData = jsonString,
                name = name,
                duration = duration,
                frameRate = frameRate,
                loops = false
            )
        }.getOrNull()
    }

    fun validateLottieJson(jsonString: String): Boolean {
        return runCatching {
            val json = JSONObject(jsonString)
            json.has("v") && json.has("fr") && json.has("layers")
        }.getOrDefault(false)
    }
}
