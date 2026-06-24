package com.example.stocatz.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.stocatz.barcode.BarcodeGenerator
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Analizza i frame della fotocamera e segnala il primo codice a barre riconosciuto
 * di un formato che siamo in grado di ridisegnare.
 */
@OptIn(ExperimentalGetImage::class)
private class BarcodeAnalyzer(
    private val onDetected: (value: String, format: String) -> Unit
) : ImageAnalysis.Analyzer, AutoCloseable {

    private val scanner = BarcodeScanning.getClient()
    private val done = AtomicBoolean(false)

    override fun analyze(imageProxy: ImageProxy) {
        if (done.get()) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val value = barcode.rawValue ?: continue
                    val format = BarcodeGenerator.mlKitFormatToZXingName(barcode.format) ?: continue
                    if (done.compareAndSet(false, true)) {
                        onDetected(value, format)
                    }
                    break
                }
            }
            .addOnFailureListener { Log.e("BarcodeAnalyzer", "Scansione fallita", it) }
            .addOnCompleteListener { imageProxy.close() }
    }

    override fun close() = scanner.close()
}

/**
 * Mostra l'anteprima della fotocamera a tutto schermo e richiama [onBarcodeDetected]
 * appena riconosce un codice a barre valido.
 */
@Composable
fun ScannerScreen(
    onBarcodeDetected: (value: String, format: String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val cameraProvider = cameraProviderFuture.get()
        val analyzer = BarcodeAnalyzer(onBarcodeDetected)

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, analyzer) }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        } catch (e: Exception) {
            Log.e("ScannerScreen", "Impossibile avviare la fotocamera", e)
        }

        onDispose {
            cameraProvider.unbindAll()
            analyzer.close()          // chiude il client ML Kit
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.safeDrawingPadding().fillMaxWidth()) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Chiudi",
                    tint = Color.White
                )
            }
        }

        Text(
            text = "Inquadra il codice a barre della carta",
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .fillMaxWidth()
                .padding(24.dp)
        )
    }
}
