package com.example.stocatz.barcode

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

/**
 * Disegna i codici a barre a partire dal valore salvato (non sono foto: vengono
 * rigenerati vettorialmente con ZXing, così risultano sempre nitidi e scansionabili).
 */
object BarcodeGenerator {

    /** I formati 2D occupano un quadrato; i 1D sono strisce larghe e basse. */
    fun isTwoDimensional(format: String): Boolean = when (runCatching {
        BarcodeFormat.valueOf(format)
    }.getOrNull()) {
        BarcodeFormat.QR_CODE,
        BarcodeFormat.DATA_MATRIX,
        BarcodeFormat.AZTEC,
        BarcodeFormat.PDF_417 -> true
        else -> false
    }

    /**
     * Genera il bitmap del codice a barre.
     * @return il bitmap, oppure null se il valore non è valido per quel formato.
     */
    fun generate(value: String, format: String, widthPx: Int, heightPx: Int): Bitmap? {
        if (value.isBlank() || widthPx <= 0 || heightPx <= 0) return null
        val zxingFormat = runCatching { BarcodeFormat.valueOf(format) }.getOrNull() ?: return null

        return runCatching {
            val hints = mapOf(EncodeHintType.MARGIN to 2)
            val matrix = MultiFormatWriter().encode(value, zxingFormat, widthPx, heightPx, hints)
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        }.getOrNull()
    }

    /**
     * Converte il formato di ML Kit (rilevato in fase di scansione) nel nome del
     * [BarcodeFormat] di ZXing, così possiamo ridisegnarlo identico.
     * @return il nome del formato, oppure null se non è supportato per la rigenerazione.
     */
    fun mlKitFormatToZXingName(mlKitFormat: Int): String? = when (mlKitFormat) {
        Barcode.FORMAT_CODE_128 -> BarcodeFormat.CODE_128
        Barcode.FORMAT_CODE_39 -> BarcodeFormat.CODE_39
        Barcode.FORMAT_CODE_93 -> BarcodeFormat.CODE_93
        Barcode.FORMAT_CODABAR -> BarcodeFormat.CODABAR
        Barcode.FORMAT_EAN_13 -> BarcodeFormat.EAN_13
        Barcode.FORMAT_EAN_8 -> BarcodeFormat.EAN_8
        Barcode.FORMAT_ITF -> BarcodeFormat.ITF
        Barcode.FORMAT_UPC_A -> BarcodeFormat.UPC_A
        Barcode.FORMAT_UPC_E -> BarcodeFormat.UPC_E
        Barcode.FORMAT_QR_CODE -> BarcodeFormat.QR_CODE
        Barcode.FORMAT_DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
        Barcode.FORMAT_AZTEC -> BarcodeFormat.AZTEC
        Barcode.FORMAT_PDF417 -> BarcodeFormat.PDF_417
        else -> null
    }?.name

    /** Etichetta leggibile del formato, da mostrare nella UI. */
    fun humanReadableFormat(format: String): String = when (format) {
        "EAN_13" -> "EAN-13"
        "EAN_8" -> "EAN-8"
        "UPC_A" -> "UPC-A"
        "UPC_E" -> "UPC-E"
        "CODE_128" -> "Code 128"
        "CODE_39" -> "Code 39"
        "CODE_93" -> "Code 93"
        "CODABAR" -> "Codabar"
        "ITF" -> "ITF"
        "QR_CODE" -> "QR Code"
        "DATA_MATRIX" -> "Data Matrix"
        "AZTEC" -> "Aztec"
        "PDF_417" -> "PDF417"
        else -> format
    }
}
