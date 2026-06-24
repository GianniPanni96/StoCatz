package com.example.stocatz.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.example.stocatz.barcode.BarcodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Disegna il codice a barre a partire dal valore salvato.
 * La generazione ZXing avviene su Dispatchers.Default per non bloccare il main thread;
 * finché il bitmap non è pronto viene mostrato un placeholder dello stesso spazio.
 */
@Composable
fun BarcodeImage(
    value: String,
    format: String,
    modifier: Modifier = Modifier
) {
    val twoDimensional = BarcodeGenerator.isTwoDimensional(format)

    // Reset del bitmap se value/format cambiano; larghezza determinata dopo il layout
    var bitmap by remember(value, format) { mutableStateOf<Bitmap?>(null) }
    var widthPx by remember { mutableIntStateOf(0) }

    // Generazione su thread di background per non bloccare la composizione
    LaunchedEffect(value, format, widthPx) {
        if (widthPx <= 0) return@LaunchedEffect
        val heightPx = if (twoDimensional) widthPx else (widthPx / 3).coerceAtLeast(160)
        bitmap = withContext(Dispatchers.Default) {
            BarcodeGenerator.generate(value, format, widthPx, heightPx)
        }
    }

    Box(
        modifier = modifier
            .background(Color.White)
            .padding(12.dp)
            .onSizeChanged { size ->
                // Aggiorna solo se la larghezza effettiva cambia (evita loop)
                if (size.width > 0 && size.width != widthPx) widthPx = size.width
            },
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        when {
            bmp != null -> Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Codice a barre",
                contentScale = ContentScale.FillWidth,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxWidth()
            )
            widthPx == 0 -> {
                // Placeholder prima che il layout misuri la larghezza
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (twoDimensional) Modifier.fillMaxSize() else Modifier.height(80.dp))
                )
            }
            else -> {
                // Generazione in corso: Text nascosto ma spazio mantenuto
                Text(
                    text = "…",
                    color = Color.Transparent
                )
            }
        }
    }
}
