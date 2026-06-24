package com.example.stocatz.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// ─── Conversioni ─────────────────────────────────────────────────────────────

internal fun hsvToArgb(h: Float, s: Float, v: Float): Int =
    AndroidColor.HSVToColor(floatArrayOf(h, s, v)) or (0xFF shl 24)

internal fun argbToHsv(color: Int): FloatArray =
    FloatArray(3).also { AndroidColor.colorToHSV(color, it) }

internal fun argbToHex(color: Int): String = "%06X".format(color and 0xFFFFFF)

internal fun hexToArgb(hex: String): Int? {
    val clean = hex.filter { it.isLetterOrDigit() }.uppercase().take(6)
    return if (clean.length == 6) runCatching { (0xFF shl 24) or clean.toInt(16) }.getOrNull()
    else null
}

// ─── Composable principale ───────────────────────────────────────────────────

/**
 * Picker HSV con:
 * - gradiente saturation/value (box 2D)
 * - slider hue (rainbow)
 * - anteprima colore + campo hex
 *
 * Lo stato interno è inizializzato da [initialColor] una sola volta; le modifiche
 * vengono propagate tramite [onColorChanged] senza feedback loop.
 */
@Composable
fun AdvancedColorPicker(
    initialColor: Int,
    onColorChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val initHsv = remember { argbToHsv(initialColor) }
    var hue by remember { mutableFloatStateOf(initHsv[0]) }
    var sat by remember { mutableFloatStateOf(initHsv[1]) }
    var bri by remember { mutableFloatStateOf(initHsv[2]) }
    var hexText by remember { mutableStateOf(argbToHex(initialColor)) }

    fun push(h: Float, s: Float, v: Float) {
        hue = h; sat = s; bri = v
        val c = hsvToArgb(h, s, v)
        hexText = argbToHex(c)
        onColorChanged(c)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── Box sat/luminosità ────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    val w = { size.width.toFloat().coerceAtLeast(1f) }
                    val h = { size.height.toFloat().coerceAtLeast(1f) }
                    fun handle(pos: Offset) =
                        push(hue, (pos.x / w()).coerceIn(0f, 1f), 1f - (pos.y / h()).coerceIn(0f, 1f))
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        handle(down.position); down.consume()
                        while (true) {
                            val ev = awaitPointerEvent()
                            ev.changes.firstOrNull()?.also { handle(it.position); it.consume() }
                            if (ev.changes.none { it.pressed }) break
                        }
                    }
                }
        ) {
            val hueColor = Color(hsvToArgb(hue, 1f, 1f))
            // bianco → colore puro (asse X = saturazione)
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
            // trasparente → nero (asse Y = luminosità)
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            // thumb
            val tx = sat * size.width
            val ty = (1f - bri) * size.height
            val r = 10.dp.toPx()
            drawCircle(Color.White, r + 3f, Offset(tx, ty), style = Stroke(3f))
            drawCircle(Color(hsvToArgb(hue, sat, bri)), r, Offset(tx, ty))
        }

        // ── Slider hue ────────────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    val w = { size.width.toFloat().coerceAtLeast(1f) }
                    fun handle(pos: Offset) =
                        push((pos.x / w() * 360f).coerceIn(0f, 360f), sat, bri)
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        handle(down.position); down.consume()
                        while (true) {
                            val ev = awaitPointerEvent()
                            ev.changes.firstOrNull()?.also { handle(it.position); it.consume() }
                            if (ev.changes.none { it.pressed }) break
                        }
                    }
                }
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Red,
                        Color(0xFFFF7F00.toInt()), // arancione
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    )
                )
            )
            val tx = hue / 360f * size.width
            val r = size.height / 2f
            drawCircle(Color.White, r - 2.dp.toPx(), Offset(tx, r), style = Stroke(3.dp.toPx()))
        }

        // ── Anteprima + hex ───────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(hsvToArgb(hue, sat, bri)))
            )
            OutlinedTextField(
                value = hexText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isLetterOrDigit() }.uppercase().take(6)
                    hexText = clean
                    hexToArgb(clean)?.let { c ->
                        val hsv = argbToHsv(c)
                        hue = hsv[0]; sat = hsv[1]; bri = hsv[2]
                        onColorChanged(c)
                    }
                },
                label = { Text("Hex") },
                prefix = { Text("#") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Dialog wrapper ──────────────────────────────────────────────────────────

@Composable
fun AdvancedColorPickerDialog(
    initialColor: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var picked by remember { mutableStateOf(initialColor) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Colore personalizzato", style = MaterialTheme.typography.titleMedium)
                AdvancedColorPicker(
                    initialColor = initialColor,
                    onColorChanged = { picked = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Annulla") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(picked) }) { Text("Applica") }
                }
            }
        }
    }
}
