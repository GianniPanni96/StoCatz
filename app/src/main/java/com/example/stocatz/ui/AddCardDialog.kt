package com.example.stocatz.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.stocatz.barcode.BarcodeGenerator

@Composable
fun AddCardDialog(
    value: String,
    format: String,
    onConfirm: (name: String, bgColor: Int, textColor: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bgColor by remember { mutableIntStateOf(DEFAULT_CARD_BACKGROUND) }
    var txtColor by remember { mutableIntStateOf(DEFAULT_CARD_TEXT) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Nuova carta", style = MaterialTheme.typography.titleLarge)

                // Anteprima colori
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

                // Codice riconosciuto
                Text(
                    text = "${BarcodeGenerator.humanReadableFormat(format)}: $value",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                    TextButton(onClick = onDismiss) { Text("Annulla") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(name, bgColor, txtColor) }) { Text("Salva") }
                }
            }
        }
    }
}
