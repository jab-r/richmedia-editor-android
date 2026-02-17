package com.loxation.richmedia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.loxation.richmedia.util.fromHex
import com.loxation.richmedia.util.toHex

private data class PresetColor(val name: String, val hex: String)

private val presetColors = listOf(
    PresetColor("White", "#FFFFFF"),
    PresetColor("Black", "#000000"),
    PresetColor("Red", "#FF0000"),
    PresetColor("Orange", "#FF9500"),
    PresetColor("Yellow", "#FFCC00"),
    PresetColor("Green", "#34C759"),
    PresetColor("Blue", "#007AFF"),
    PresetColor("Purple", "#AF52DE"),
    PresetColor("Pink", "#FF2D55")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerRow(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(presetColors) { preset ->
            val isSelected = selectedColor.equals(preset.hex, ignoreCase = true)
            ColorCircle(
                color = fromHex(preset.hex),
                isSelected = isSelected,
                onClick = { onColorSelected(preset.hex) }
            )
        }
        item {
            // Custom color circle
            val isCustom = presetColors.none { it.hex.equals(selectedColor, ignoreCase = true) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCustom) fromHex(selectedColor) else Color.Gray.copy(alpha = 0.3f)
                    )
                    .then(
                        if (isCustom) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    .clickable { showCustomPicker = true }
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Custom color",
                    modifier = Modifier.size(16.dp),
                    tint = if (isCustom) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showCustomPicker) {
        CustomColorDialog(
            initialColor = selectedColor,
            onColorSelected = {
                onColorSelected(it)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false }
        )
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else if (color == Color.White || color == Color(0xFFFFFFFF))
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                modifier = Modifier.size(16.dp),
                tint = if (color == Color.White || color == Color(0xFFFFCC00)) Color.Black else Color.White
            )
        }
    }
}

@Composable
private fun CustomColorDialog(
    initialColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hexInput by remember { mutableStateOf(initialColor.removePrefix("#")) }
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(1f) }

    // Initialize HSB from the initial color
    LaunchedEffect(Unit) {
        val color = fromHex(initialColor)
        val hsb = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
            hsb
        )
        hue = hsb[0]
        saturation = hsb[1]
        brightness = hsb[2]
    }

    fun currentColor(): Color {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
        return Color(rgb)
    }

    fun currentHex(): String = currentColor().toHex()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor())
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )

                // Hue slider
                Text("Hue", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        hexInput = currentHex().removePrefix("#")
                    },
                    valueRange = 0f..360f
                )

                // Saturation slider
                Text("Saturation", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = saturation,
                    onValueChange = {
                        saturation = it
                        hexInput = currentHex().removePrefix("#")
                    },
                    valueRange = 0f..1f
                )

                // Brightness slider
                Text("Brightness", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        hexInput = currentHex().removePrefix("#")
                    },
                    valueRange = 0f..1f
                )

                // Hex input
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input.filter { it.isLetterOrDigit() }.take(6)
                        if (hexInput.length == 6) {
                            runCatching {
                                val color = fromHex("#$hexInput")
                                val hsb = FloatArray(3)
                                android.graphics.Color.RGBToHSV(
                                    (color.red * 255).toInt(),
                                    (color.green * 255).toInt(),
                                    (color.blue * 255).toInt(),
                                    hsb
                                )
                                hue = hsb[0]
                                saturation = hsb[1]
                                brightness = hsb[2]
                            }
                        }
                    },
                    label = { Text("Hex") },
                    prefix = { Text("#") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (hexInput.length == 6) {
                                onColorSelected("#$hexInput")
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentHex()) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
