package com.example.stocatz.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.stocatz.barcode.BarcodeGenerator
import com.example.stocatz.barcode.PhotoImportItem

@Composable
fun PhotoImportDialog(
    item: PhotoImportItem,
    currentIndex: Int,
    total: Int,
    onSave: (name: String, bgColor: Int, textColor: Int) -> Unit,
    onSkip: () -> Unit
) {
    // reset dei campi ad ogni nuova card (chiave = indice corrente)
    var name by remember(currentIndex) { mutableStateOf("") }
    var bgColor by remember(currentIndex) { mutableIntStateOf(DEFAULT_CARD_BACKGROUND) }
    var txtColor by remember(currentIndex) { mutableIntStateOf(DEFAULT_CARD_TEXT) }

    Dialog(onDismissRequest = {}) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Intestazione con avanzamento
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Carta ${currentIndex + 1} di $total",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${currentIndex + 1}/$total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { (currentIndex + 1f) / total },
                    modifier = Modifier.fillMaxWidth()
                )

                // Anteprima foto
                item.thumbnail?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Foto sorgente",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                // Codice rilevato
                Text(
                    text = "${BarcodeGenerator.humanReadableFormat(item.format)}: ${item.value}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Anteprima carta con colori scelti
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(bgColor)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = name.ifBlank { "Anteprima carta" },
                        color = Color(txtColor),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome carta (es. Esselunga)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ColorPickerSection(
                    label = "Colore sfondo",
                    colors = BACKGROUND_COLORS,
                    selectedColor = bgColor,
                    onColorSelected = { bgColor = it }
                )
                ColorPickerSection(
                    label = "Colore testo",
                    colors = TEXT_COLORS,
                    selectedColor = txtColor,
                    onColorSelected = { txtColor = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) { Text("Salta") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(name, bgColor, txtColor) }) { Text("Salva") }
                }
            }
        }
    }
}
