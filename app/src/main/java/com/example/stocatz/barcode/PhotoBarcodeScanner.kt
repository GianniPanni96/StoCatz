package com.example.stocatz.barcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PhotoImportItem(
    val uri: Uri,
    val value: String,
    val format: String,
    val thumbnail: Bitmap?
)

/**
 * Per ogni Uri nella lista: decodifica l'immagine, cerca un codice a barre con ML Kit,
 * scarta duplicati (sia tra le foto selezionate sia rispetto alle carte già salvate).
 * Restituisce solo le voci con un barcode valido e non duplicato.
 */
suspend fun scanPhotosForBarcodes(
    context: Context,
    uris: List<Uri>,
    existingValues: Set<String>
): List<PhotoImportItem> = withContext(Dispatchers.IO) {
    val scanner = BarcodeScanning.getClient()
    val results = mutableListOf<PhotoImportItem>()
    val seenValues = existingValues.toMutableSet()

    for (uri in uris) {
        runCatching {
            val bitmap = loadScaledBitmap(context, uri) ?: return@runCatching
            val barcodes = suspendCancellableCoroutine { cont ->
                scanner.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            val barcode = barcodes.firstOrNull { it.rawValue != null } ?: return@runCatching
            val value = barcode.rawValue!!
            val format = BarcodeGenerator.mlKitFormatToZXingName(barcode.format)
                ?: return@runCatching
            if (value !in seenValues) {
                seenValues += value
                results += PhotoImportItem(uri, value, format, bitmap)
            }
        }
    }
    scanner.close()
    results
}

private fun loadScaledBitmap(context: Context, uri: Uri, targetSize: Int = 800): Bitmap? =
    runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        val maxDim = maxOf(opts.outWidth, opts.outHeight).coerceAtLeast(1)
        opts.inSampleSize = if (maxDim > targetSize) Integer.highestOneBit(maxDim / targetSize) else 1
        opts.inJustDecodeBounds = false
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }.getOrNull()
