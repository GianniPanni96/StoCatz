package com.example.stocatz.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.stocatz.barcode.BarcodeGenerator

/**
 * Disegna il codice a barre a partire dal valore salvato.
 * Il codice viene rigenerato con ZXing (non è una foto): resta sempre nitido
 * ed è perfettamente scansionabile dal lettore di cassa.
 */
@Composable
fun BarcodeImage(
    value: String,
    format: String,
    modifier: Modifier = Modifier
) {
    val twoDimensional = BarcodeGenerator.isTwoDimensional(format)

    BoxWithConstraints(
        modifier = modifier
            .background(Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = if (constraints.hasBoundedWidth && constraints.maxWidth in 1..4000) {
            constraints.maxWidth
        } else {
            900
        }
        val heightPx = if (twoDimensional) widthPx else (widthPx / 3).coerceAtLeast(160)

        val bitmap = remember(value, format, widthPx, heightPx) {
            BarcodeGenerator.generate(value, format, widthPx, heightPx)
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Codice a barre",
                contentScale = ContentScale.FillWidth,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "Impossibile generare il codice a barre per questo valore.",
                color = Color.Black
            )
        }
    }
}
