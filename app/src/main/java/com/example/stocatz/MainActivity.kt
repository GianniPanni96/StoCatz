package com.example.stocatz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stocatz.barcode.PhotoImportItem
import com.example.stocatz.barcode.scanPhotosForBarcodes
import com.example.stocatz.data.LoyaltyCard
import com.example.stocatz.ui.AddCardDialog
import com.example.stocatz.ui.CardDetailScreen
import com.example.stocatz.ui.CardListScreen
import com.example.stocatz.ui.CardViewModel
import com.example.stocatz.ui.PhotoImportDialog
import com.example.stocatz.ui.ScannerScreen
import com.example.stocatz.ui.theme.StoCatzTheme
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object List : Screen
    data object Scan : Screen
    data class Detail(val cardId: String) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StoCatzTheme {
                StoCatzApp()
            }
        }
    }
}

@Composable
private fun StoCatzApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: CardViewModel = viewModel()

    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var screen by remember { mutableStateOf<Screen>(Screen.List) }
    var pendingScan by remember { mutableStateOf<Pair<String, String>?>(null) }

    // --- Stato import da foto ---
    var isProcessingPhotos by remember { mutableStateOf(false) }
    var pendingImports by remember { mutableStateOf<List<PhotoImportItem>>(emptyList()) }
    var currentImportIndex by remember { mutableIntStateOf(0) }
    var showNoBarcodesFound by remember { mutableStateOf(false) }

    // --- Launcher fotocamera ---
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) screen = Screen.Scan }

    // --- Launcher galleria / file ---
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        isProcessingPhotos = true
        scope.launch {
            val existingValues = allCards.map { it.value }.toSet()
            val items = scanPhotosForBarcodes(context, uris, existingValues)
            isProcessingPhotos = false
            if (items.isEmpty()) {
                showNoBarcodesFound = true
            } else {
                pendingImports = items
                currentImportIndex = 0
            }
        }
    }

    // Permesso storage per Android ≤ 12; da Android 13 il Photo Picker non lo richiede
    val storagePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) photoPickerLauncher.launch("image/*") }

    fun startScan() {
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (ok) screen = Screen.Scan else cameraPermLauncher.launch(Manifest.permission.CAMERA)
    }

    fun startPhotoImport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            photoPickerLauncher.launch("image/*")
        } else {
            val ok = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (ok) photoPickerLauncher.launch("image/*")
            else storagePermLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // --- Navigazione principale ---
    when (val current = screen) {
        Screen.List -> CardListScreen(
            cards = cards,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::setSearchQuery,
            onAddByScan = ::startScan,
            onAddByPhoto = ::startPhotoImport,
            onCardClick = { screen = Screen.Detail(it.id) }
        )

        Screen.Scan -> ScannerScreen(
            onBarcodeDetected = { value, format ->
                pendingScan = value to format
                screen = Screen.List
            },
            onClose = { screen = Screen.List }
        )

        is Screen.Detail -> {
            val card: LoyaltyCard? = allCards.firstOrNull { it.id == current.cardId }
            if (card == null) {
                screen = Screen.List
            } else {
                CardDetailScreen(
                    card = card,
                    onBack = { screen = Screen.List },
                    onDelete = {
                        viewModel.deleteCard(card.id)
                        screen = Screen.List
                    },
                    onUpdateColors = { bg, txt ->
                        viewModel.updateCardColors(card.id, bg, txt)
                    }
                )
            }
        }
    }

    // --- Dialog aggiunta da scansione ---
    pendingScan?.let { (value, format) ->
        AddCardDialog(
            value = value,
            format = format,
            onConfirm = { name, bgColor, txtColor ->
                viewModel.addCard(name, value, format, bgColor, txtColor)
                pendingScan = null
            },
            onDismiss = { pendingScan = null }
        )
    }

    // --- Dialog import da foto (sequenziale) ---
    if (pendingImports.isNotEmpty() && currentImportIndex < pendingImports.size) {
        val item = pendingImports[currentImportIndex]

        fun advance() {
            if (currentImportIndex + 1 >= pendingImports.size) {
                pendingImports = emptyList()
                currentImportIndex = 0
            } else {
                currentImportIndex++
            }
        }

        PhotoImportDialog(
            item = item,
            currentIndex = currentImportIndex,
            total = pendingImports.size,
            onSave = { name, bgColor, txtColor ->
                viewModel.addCard(name, item.value, item.format, bgColor, txtColor)
                advance()
            },
            onSkip = { advance() }
        )
    }

    // --- Dialogs di stato ---
    if (isProcessingPhotos) {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(16.dp))
                    Text("Analisi foto in corso…")
                }
            }
        }
    }

    if (showNoBarcodesFound) {
        AlertDialog(
            onDismissRequest = { showNoBarcodesFound = false },
            title = { Text("Nessuna carta trovata") },
            text = {
                Text(
                    "Non sono stati trovati codici a barre validi nelle foto selezionate, " +
                    "oppure erano già presenti nell'app."
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoBarcodesFound = false }) { Text("OK") }
            }
        )
    }
}
