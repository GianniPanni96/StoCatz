package com.example.stocatz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stocatz.data.LoyaltyCard
import com.example.stocatz.ui.AddCardDialog
import com.example.stocatz.ui.CardDetailScreen
import com.example.stocatz.ui.CardListScreen
import com.example.stocatz.ui.CardViewModel
import com.example.stocatz.ui.ScannerScreen
import com.example.stocatz.ui.theme.StoCatzTheme

/** Le schermate dell'app. */
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
    val viewModel: CardViewModel = viewModel()
    val cards by viewModel.cards.collectAsStateWithLifecycle()

    var screen by remember { mutableStateOf<Screen>(Screen.List) }
    // Risultato della scansione in attesa di un nome (mostra il dialog di salvataggio).
    var pendingScan by remember { mutableStateOf<Pair<String, String>?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) screen = Screen.Scan
    }

    fun startScan() {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            screen = Screen.Scan
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    when (val current = screen) {
        Screen.List -> CardListScreen(
            cards = cards,
            onAddClick = { startScan() },
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
            val card: LoyaltyCard? = cards.firstOrNull { it.id == current.cardId }
            if (card == null) {
                // La carta non esiste più (es. eliminata): torna all'elenco.
                screen = Screen.List
            } else {
                CardDetailScreen(
                    card = card,
                    onBack = { screen = Screen.List },
                    onDelete = {
                        viewModel.deleteCard(card.id)
                        screen = Screen.List
                    }
                )
            }
        }
    }

    pendingScan?.let { (value, format) ->
        AddCardDialog(
            value = value,
            format = format,
            onConfirm = { name ->
                viewModel.addCard(name = name, value = value, format = format)
                pendingScan = null
            },
            onDismiss = { pendingScan = null }
        )
    }
}
