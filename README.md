# StoCatz

Portafoglio digitale per carte fedeltà fisiche. Scansiona il codice a barre di una carta con la fotocamera e StoCatz la salva sul telefono, pronta da mostrare alla cassa.

## Funzionalità

- **Scansione** — inquadra il codice a barre di una carta fedeltà con la fotocamera posteriore; l'app lo riconosce automaticamente
- **Carta virtuale** — il codice viene ridisegnato vettorialmente (non è una foto), sempre nitido e scansionabile dal lettore di cassa
- **Personalizzazione** — scegli il colore di sfondo e del testo per ogni carta, sia in fase di aggiunta che dal dettaglio; sono disponibili 12 colori per lo sfondo e una palette completa per il testo
- **Ricerca** — barra di ricerca nella lista che filtra per nome e per valore del codice
- **Luminosità automatica** — quando apri il dettaglio di una carta lo schermo va al massimo della luminosità per facilitare la lettura al POS
- **Persistenza locale** — le carte vengono salvate sul dispositivo senza necessità di account o connessione internet
- **Navigazione** — tasto indietro di sistema funzionante in tutte le schermate

## Formati di codice a barre supportati

| Formato | Esempio di utilizzo |
|---------|-------------------|
| EAN-13 / EAN-8 | Supermercati italiani (Esselunga, Conad, …) |
| UPC-A / UPC-E | Grande distribuzione |
| Code 128 / Code 39 / Code 93 | Carte fedeltà generiche |
| ITF / Codabar | Logistica e farmacia |
| QR Code | App e servizi digitali |
| Data Matrix / Aztec / PDF417 | Biglietti e documenti |

## Stack tecnologico

| Componente | Libreria |
|-----------|---------|
| UI | Jetpack Compose + Material 3 |
| Fotocamera | CameraX 1.4 |
| Riconoscimento barcode | ML Kit Barcode Scanning |
| Generazione barcode | ZXing Core 3.5 |
| Persistenza | DataStore Preferences |
| Serializzazione | kotlinx.serialization |
| Architettura | ViewModel + StateFlow |

## Struttura del progetto

```
app/src/main/java/com/example/stocatz/
├── MainActivity.kt              # Entry point, navigazione tra schermate
├── barcode/
│   └── BarcodeGenerator.kt     # Generazione e mapping dei formati barcode
├── data/
│   ├── LoyaltyCard.kt          # Modello dati (id, nome, valore, formato, colori)
│   └── CardRepository.kt       # Lettura/scrittura su DataStore
└── ui/
    ├── CardViewModel.kt         # Logica di business, ricerca, aggiornamento colori
    ├── ColorPicker.kt           # Palette colori e composable di selezione
    ├── BarcodeImage.kt          # Composable che disegna il barcode vettoriale
    ├── CardListScreen.kt        # Lista carte con barra di ricerca
    ├── CardDetailScreen.kt      # Dettaglio carta, barcode a schermo intero, picker colori
    ├── AddCardDialog.kt         # Dialog post-scansione: nome + colori
    └── ScannerScreen.kt         # Anteprima fotocamera + analisi frame
```

## Requisiti

- Android 7.0 (API 24) o superiore
- Fotocamera posteriore
- Nessuna connessione internet necessaria

## Build

```bash
# Con Android Studio: File → Sync Project with Gradle Files, poi Run ▶
# Da riga di comando (richiede JAVA_HOME configurato):
./gradlew assembleDebug
```

L'APK debug viene generato in `app/build/outputs/apk/debug/app-debug.apk`.

## Permessi richiesti

| Permesso | Motivo |
|---------|--------|
| `CAMERA` | Scansione del codice a barre |

Nessun altro permesso: tutto rimane sul dispositivo.
