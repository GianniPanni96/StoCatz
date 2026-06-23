package com.example.stocatz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

val BACKGROUND_COLORS: List<Int> = listOf(
    0xFF1565C0.toInt(), // Blu
    0xFF283593.toInt(), // Indaco
    0xFF6A1B9A.toInt(), // Viola
    0xFFAD1457.toInt(), // Rosa
    0xFFB71C1C.toInt(), // Rosso
    0xFFBF360C.toInt(), // Arancione scuro
    0xFF2E7D32.toInt(), // Verde
    0xFF00695C.toInt(), // Verde acqua
    0xFF37474F.toInt(), // Ardesia
    0xFF212121.toInt(), // Nero
    0xFFEEEEEE.toInt(), // Bianco grigio
    0xFFFFD54F.toInt(), // Oro
)

val TEXT_COLORS: List<Int> = listOf(
    0xFFFFFFFF.toInt(), // Bianco
    0xFF212121.toInt(), // Nero
)

val DEFAULT_CARD_BACKGROUND: Int = 0xFF1565C0.toInt()
val DEFAULT_CARD_TEXT: Int = 0xFFFFFFFF.toInt()

@Composable
fun ColorSwatchRow(
    colors: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { colorInt ->
            val color = Color(colorInt)
            val isSelected = colorInt == selected
            val checkTint = if (color.luminance() > 0.4f) Color.Black else Color.White

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected)
                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else
                            Modifier
                    )
                    .clickable { onSelect(colorInt) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = checkTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPickerSection(
    label: String,
    colors: List<Int>,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        ColorSwatchRow(colors = colors, selected = selectedColor, onSelect = onColorSelected)
    }
}
