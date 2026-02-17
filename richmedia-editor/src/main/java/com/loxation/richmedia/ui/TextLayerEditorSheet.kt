package com.loxation.richmedia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loxation.richmedia.model.*
import com.loxation.richmedia.util.fromHex

/**
 * Modal bottom sheet for editing text layer properties:
 * text, font, size, color, formatting, alignment, shadow, outline, animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextLayerEditorSheet(
    layer: TextLayer,
    onSave: (TextLayer) -> Unit,
    onDismiss: () -> Unit,
    onDrawPath: (() -> Unit)? = null
) {
    var editedLayer by remember { mutableStateOf(layer) }
    var showAnimationPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text("Edit Text Layer", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onSave(editedLayer) }) { Text("Save") }
            }

            Spacer(Modifier.height(16.dp))

            // --- Text ---
            Text("Text", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = editedLayer.text,
                onValueChange = { editedLayer = editedLayer.copy(text = it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6
            )

            Spacer(Modifier.height(16.dp))

            // --- Font ---
            Text("Font", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            val fonts = listOf("System", "Georgia", "Helvetica", "Courier", "Times New Roman")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                fonts.forEachIndexed { index, font ->
                    SegmentedButton(
                        selected = editedLayer.style.font == font,
                        onClick = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(font = font)) },
                        shape = SegmentedButtonDefaults.itemShape(index, fonts.size)
                    ) {
                        Text(font, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Text Color ---
            Text("Text Color", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            ColorPickerRow(
                selectedColor = editedLayer.style.color,
                onColorSelected = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(color = it)) }
            )

            Spacer(Modifier.height(12.dp))

            // --- Size ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Size: ${editedLayer.style.size.toInt()}pt", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    val newSize = (editedLayer.style.size - 2).coerceAtLeast(10f)
                    editedLayer = editedLayer.copy(style = editedLayer.style.copy(size = newSize))
                }) { Text("-", fontSize = 20.sp) }
                IconButton(onClick = {
                    val newSize = (editedLayer.style.size + 2).coerceAtMost(72f)
                    editedLayer = editedLayer.copy(style = editedLayer.style.copy(size = newSize))
                }) { Text("+", fontSize = 20.sp) }
            }

            Spacer(Modifier.height(8.dp))

            // --- Formatting toggles ---
            Text("Formatting", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = editedLayer.style.bold,
                    onClick = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(bold = !editedLayer.style.bold)) },
                    label = { Text("Bold", fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = editedLayer.style.italic,
                    onClick = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(italic = !editedLayer.style.italic)) },
                    label = { Text("Italic", fontStyle = FontStyle.Italic) }
                )
                FilterChip(
                    selected = editedLayer.style.underline,
                    onClick = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(underline = !editedLayer.style.underline)) },
                    label = { Text("Underline") }
                )
            }

            Spacer(Modifier.height(8.dp))

            // --- Alignment ---
            Text("Alignment", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val alignments = listOf(TextAlignment.left to "Left", TextAlignment.center to "Center", TextAlignment.right to "Right")
                alignments.forEachIndexed { index, (align, label) ->
                    SegmentedButton(
                        selected = editedLayer.style.align == align,
                        onClick = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(align = align)) },
                        shape = SegmentedButtonDefaults.itemShape(index, alignments.size)
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Shadow ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shadow", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = editedLayer.style.shadow != null,
                    onCheckedChange = { enabled ->
                        editedLayer = editedLayer.copy(
                            style = editedLayer.style.copy(
                                shadow = if (enabled) TextShadow() else null
                            )
                        )
                    }
                )
            }

            if (editedLayer.style.shadow != null) {
                val shadow = editedLayer.style.shadow!!
                Text("Shadow Color", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(2.dp))
                ColorPickerRow(
                    selectedColor = shadow.color,
                    onColorSelected = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(shadow = shadow.copy(color = it))) }
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Radius: ${shadow.radius.toInt()}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Slider(
                        value = shadow.radius,
                        onValueChange = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(shadow = shadow.copy(radius = it))) },
                        valueRange = 0f..20f,
                        modifier = Modifier.width(200.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Opacity: ${(shadow.opacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Slider(
                        value = shadow.opacity,
                        onValueChange = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(shadow = shadow.copy(opacity = it))) },
                        valueRange = 0f..1f,
                        modifier = Modifier.width(200.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // --- Outline ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Outline", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = editedLayer.style.outline != null,
                    onCheckedChange = { enabled ->
                        editedLayer = editedLayer.copy(
                            style = editedLayer.style.copy(
                                outline = if (enabled) TextOutline() else null
                            )
                        )
                    }
                )
            }

            if (editedLayer.style.outline != null) {
                val outline = editedLayer.style.outline!!
                Text("Outline Color", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(2.dp))
                ColorPickerRow(
                    selectedColor = outline.color,
                    onColorSelected = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(outline = outline.copy(color = it))) }
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Width: ${outline.width.toInt()}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Slider(
                        value = outline.width,
                        onValueChange = { editedLayer = editedLayer.copy(style = editedLayer.style.copy(outline = outline.copy(width = it))) },
                        valueRange = 1f..8f,
                        modifier = Modifier.width(200.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Animation ---
            Text("Animation", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            if (editedLayer.animation != null) {
                val anim = editedLayer.animation!!
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(anim.preset.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(anim.preset.category.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showAnimationPicker = true }) { Text("Change") }
                }

                // Delay
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Delay: ${"%.1f".format(anim.delay)}s", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Slider(
                        value = anim.delay.toFloat(),
                        onValueChange = { editedLayer = editedLayer.copy(animation = anim.copy(delay = it.toDouble())) },
                        valueRange = 0f..5f,
                        modifier = Modifier.width(200.dp)
                    )
                }

                // Duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Duration: ${"%.1f".format(anim.duration)}s", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Slider(
                        value = anim.duration.toFloat(),
                        onValueChange = { editedLayer = editedLayer.copy(animation = anim.copy(duration = it.toDouble())) },
                        valueRange = 0.1f..3f,
                        modifier = Modifier.width(200.dp)
                    )
                }

                // Loop toggle for looping presets
                if (anim.preset.category == AnimationCategory.LOOP || anim.preset.category == AnimationCategory.PATH) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Loop", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = anim.loop,
                            onCheckedChange = { editedLayer = editedLayer.copy(animation = anim.copy(loop = it)) }
                        )
                    }
                }

                // Path drawing for motionPath/curvePath
                if (anim.preset == AnimationPreset.motionPath || anim.preset == AnimationPreset.curvePath) {
                    Spacer(Modifier.height(4.dp))
                    if (editedLayer.path != null) {
                        Text(
                            "${editedLayer.path!!.points.size} points \u2022 ${editedLayer.path!!.type.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    OutlinedButton(
                        onClick = { onDrawPath?.invoke() },
                        enabled = onDrawPath != null
                    ) {
                        Text(if (editedLayer.path != null) "Redraw Path" else "Draw Path")
                    }
                    Spacer(Modifier.height(4.dp))
                }

                TextButton(
                    onClick = { editedLayer = editedLayer.copy(animation = null) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove Animation")
                }
            } else {
                OutlinedButton(onClick = { showAnimationPicker = true }) {
                    Text("Add Animation")
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Preview ---
            Text("Preview", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            ) {
                PreviewText(editedLayer)
            }
        }
    }

    if (showAnimationPicker) {
        AnimationPresetPicker(
            selectedPreset = editedLayer.animation?.preset,
            onSelect = { preset ->
                editedLayer = editedLayer.copy(animation = TextAnimation(preset = preset))
                showAnimationPicker = false
            },
            onDismiss = { showAnimationPicker = false }
        )
    }
}

@Composable
private fun PreviewText(layer: TextLayer) {
    val style = layer.style
    val textColor = runCatching { fromHex(style.color) }.getOrDefault(Color.White)
    val fontFamily = when (style.font) {
        "Georgia" -> FontFamily.Serif
        "Helvetica" -> FontFamily.SansSerif
        "Courier" -> FontFamily.Monospace
        "Times New Roman" -> FontFamily.Serif
        else -> FontFamily.Default
    }
    val shadow = style.shadow?.let { s ->
        val sc = runCatching { fromHex(s.color) }.getOrDefault(Color.Black)
        Shadow(color = sc.copy(alpha = s.opacity), offset = Offset(s.offset.width, s.offset.height), blurRadius = s.radius)
    }

    Text(
        text = layer.text.ifEmpty { "Preview" },
        style = TextStyle(
            color = textColor,
            fontSize = style.size.sp,
            fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = buildList {
                if (style.underline) add(TextDecoration.Underline)
            }.let { if (it.isEmpty()) null else TextDecoration.combine(it) },
            fontFamily = fontFamily,
            textAlign = when (style.align) {
                TextAlignment.left -> TextAlign.Start
                TextAlignment.center -> TextAlign.Center
                TextAlignment.right -> TextAlign.End
            },
            shadow = shadow
        )
    )
}
