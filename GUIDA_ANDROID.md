# Guida allo sviluppo Android con Kotlin — tramite StoCatz

Questa guida usa il codice di StoCatz come filo conduttore. Ogni concetto viene introdotto con analogie a Python o C++, poi collegato al codice reale dell'app.

---

## Indice

1. [Kotlin in pillole](#1-kotlin-in-pillole)
2. [Struttura di un progetto Android](#2-struttura-di-un-progetto-android)
3. [AndroidManifest — il "contratto" dell'app](#3-androidmanifest--il-contratto-dellapp)
4. [Gradle — il build system](#4-gradle--il-build-system)
5. [Activity — il punto di ingresso](#5-activity--il-punto-di-ingresso)
6. [Jetpack Compose — UI dichiarativa](#6-jetpack-compose--ui-dichiarativa)
7. [State e Recomposition — il motore di Compose](#7-state-e-recomposition--il-motore-di-compose)
8. [ViewModel — separare logica dalla UI](#8-viewmodel--separare-logica-dalla-ui)
9. [Coroutines — concorrenza senza thread espliciti](#9-coroutines--concorrenza-senza-thread-espliciti)
10. [Flow e StateFlow — stream di dati reattivi](#10-flow-e-stateflow--stream-di-dati-reattivi)
11. [DataStore — persistenza locale](#11-datastore--persistenza-locale)
12. [CameraX e ML Kit — accesso all'hardware](#12-camerax-e-ml-kit--accesso-allhardware)
13. [Architettura complessiva](#13-architettura-complessiva)

---

## 1. Kotlin in pillole

### Tipizzazione: il meglio di entrambi i mondi

Kotlin è **fortemente tipizzato come C++**, ma il compilatore deduce i tipi quando sono ovvi (come Python con le type hint, ma obbligatorio a runtime).

```kotlin
// Kotlin: il tipo è dedotto, ma è Int a compile time
val n = 42
var nome = "StoCatz"

// Equivalente Python (solo a scopo documentativo):
# n: int = 42
# nome: str = "StoCatz"

// Equivalente C++:
// auto n = 42;
// std::string nome = "StoCatz";
```

`val` = costante (come `const` in C++, o una variabile non riassegnabile in Python).  
`var` = variabile mutabile.

### Null safety — niente più NullPointerException

In C++ un puntatore può essere `nullptr` senza che il compilatore te lo dica. In Python una variabile può essere `None` senza controllo statico. Kotlin risolve questo a livello di tipo:

```kotlin
var nome: String  = "Esselunga"   // Non può essere null
var nome: String? = null           // Può essere null — il ? lo dichiara esplicitamente

// Per usare un valore nullable devi gestire il caso null:
val lunghezza = nome?.length       // lunghezza è Int? (può essere null)
val lunghezza = nome?.length ?: 0  // Elvis operator: se null usa 0

// Equivalente Python:
# lunghezza = len(nome) if nome is not None else 0
```

In StoCatz lo vedi in [`CardRepository.kt`](app/src/main/java/com/example/stocatz/data/CardRepository.kt):
```kotlin
private fun decode(raw: String?): List<LoyaltyCard> {
    if (raw == null) return emptyList()   // gestione esplicita del null
    ...
}
```

### Data class — strutture dati senza boilerplate

```kotlin
// Kotlin — genera automaticamente equals(), hashCode(), copy(), toString()
@Serializable
data class LoyaltyCard(
    val id: String,
    val name: String,
    val backgroundColor: Int = 0xFF1565C0.toInt()  // valore di default
)
```

```python
# Python — equivalente con dataclass
from dataclasses import dataclass, field

@dataclass
class LoyaltyCard:
    id: str
    name: str
    background_color: int = 0xFF1565C0
```

```cpp
// C++ — dovresti scrivere tutto a mano oppure usare struct senza metodi
struct LoyaltyCard {
    std::string id;
    std::string name;
    int background_color = 0xFF1565C0;
};
```

Il metodo `copy()` generato automaticamente permette di creare una copia modificando solo certi campi — lo usiamo in [`CardViewModel.kt`](app/src/main/java/com/example/stocatz/ui/CardViewModel.kt):
```kotlin
repository.updateCard(card.copy(backgroundColor = backgroundColor, textColor = textColor))
// "prendi questa carta, uguale in tutto, ma con questi due campi diversi"
```

### Funzioni lambda e higher-order functions

Kotlin tratta le funzioni come valori, come Python (e come `std::function` in C++):

```kotlin
// Funzione che accetta un'altra funzione come parametro
fun filtra(lista: List<Int>, condizione: (Int) -> Boolean): List<Int> {
    return lista.filter(condizione)
}

// Uso con lambda (la lambda va fuori dalle parentesi se è l'ultimo parametro)
filtra(listOf(1, 2, 3, 4)) { n -> n > 2 }
// oppure, se la lambda ha un solo parametro, si chiama "it":
filtra(listOf(1, 2, 3, 4)) { it > 2 }
```

```python
# Python — equivalente
def filtra(lista, condizione):
    return list(filter(condizione, lista))

filtra([1, 2, 3, 4], lambda n: n > 2)
```

In StoCatz la ricerca usa questo pattern:
```kotlin
// CardViewModel.kt
list.filter {
    it.name.contains(query, ignoreCase = true) ||
    it.value.contains(query, ignoreCase = true)
}
```

### Sealed interface — enum potenziate

Una `sealed interface` (o `sealed class`) è un insieme chiuso di tipi. Simile a un `enum` in C++, ma ogni variante può portare dati diversi.

```kotlin
// MainActivity.kt
private sealed interface Screen {
    data object List : Screen      // variante senza dati
    data object Scan : Screen
    data class Detail(val cardId: String) : Screen  // variante con dati
}
```

```python
# Python — equivalente approssimativo con dataclasses
from dataclasses import dataclass
from typing import Union

@dataclass
class ScreenList: pass
@dataclass
class ScreenScan: pass
@dataclass
class ScreenDetail:
    card_id: str

Screen = Union[ScreenList, ScreenScan, ScreenDetail]
```

Il vantaggio in Kotlin: il compilatore sa che la `sealed interface` ha solo quelle varianti, quindi in un `when` ti avvisa se ne dimentichi una.

### Extension functions — aggiungere metodi senza ereditarietà

```kotlin
// Aggiunge un metodo a Context (classe di Android) senza modificarla
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Si usa come se fosse un metodo nativo di Context:
val activity = context.findActivity()
```

In Python potresti fare qualcosa di simile "monkey-patching", ma in Kotlin è type-safe e risolto a compile time.

---

## 2. Struttura di un progetto Android

```
StoCatz/
├── app/
│   ├── build.gradle.kts          ← dipendenze del modulo app
│   └── src/main/
│       ├── AndroidManifest.xml   ← "carta d'identità" dell'app
│       ├── java/…/               ← codice Kotlin
│       └── res/                  ← risorse (layout, icone, stringhe)
│           ├── drawable/         ← icone vettoriali (XML)
│           ├── mipmap-*/         ← icone launcher (PNG/WebP per densità diverse)
│           └── values/           ← colori, stringhe, temi
├── gradle/
│   └── libs.versions.toml        ← catalogo versioni dipendenze
└── build.gradle.kts              ← build del progetto radice
```

**Analogia Python:** immagina un progetto Flask o Django — hai una cartella `static/` per le risorse, un `requirements.txt` per le dipendenze, e il codice Python separato. Qui `res/` è come `static/`, `libs.versions.toml` è come `requirements.txt`.

**Analogia C++:** il `build.gradle.kts` è come un `CMakeLists.txt`, ma molto più potente — gestisce dipendenze remote, varianti di build (debug/release), firma dell'APK e molto altro.

### Le risorse (`res/`)

Android separa il codice dalla UI. I file in `res/` vengono compilati in un indice binario (classe `R`) accessibile dal codice:

```kotlin
// Nel codice Kotlin, accedi alle risorse con R.*
getString(R.string.app_name)
getDrawable(R.drawable.ic_launcher_background)
```

```python
# Equivalente concettuale in Python (tkinter):
# img = tk.PhotoImage(file="assets/icona.png")
# label.config(text="StoCatz")
```

La cartella `mipmap-hdpi/`, `mipmap-xxhdpi/`, ecc. contiene la stessa icona a densità di pixel diverse. Android sceglie automaticamente quella giusta in base al dispositivo — tu non devi preoccupartene.

---

## 3. AndroidManifest — il "contratto" dell'app

Il file [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) è una dichiarazione di ciò che l'app è e di cosa ha bisogno. Il sistema operativo lo legge prima ancora di avviare l'app.

```xml
<manifest>
    <!-- Dichiaro che voglio usare la fotocamera -->
    <uses-permission android:name="android.permission.CAMERA" />

    <application android:icon="@mipmap/sto_catz_image" ...>
        <!-- Dichiaro le Activity (schermate) che compongono l'app -->
        <activity android:name=".MainActivity" android:exported="true">
            <!-- Questa è la schermata che si apre al tap dell'icona -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**Analogia:** in Python con Flask dichiari le route (`@app.route("/")`) per dire al framework quali URL gestisci. In Android il Manifest fa qualcosa di simile: dice al SO quali schermate gestisce l'app, quali hardware richiede, e con quali permessi vuole operare.

> ⚠️ **Permessi a runtime:** dichiarare `CAMERA` nel Manifest è necessario ma non sufficiente. Da Android 6.0 in poi devi anche **chiederlo all'utente** a runtime. Lo vediamo nella sezione sulla fotocamera.

---

## 4. Gradle — il build system

[`gradle/libs.versions.toml`](gradle/libs.versions.toml) è il catalogo delle dipendenze:

```toml
[versions]
camerax = "1.4.2"
mlkitBarcode = "17.3.0"

[libraries]
androidx-camera-core = { group = "androidx.camera", name = "camera-core", version.ref = "camerax" }
mlkit-barcode-scanning = { group = "com.google.mlkit", name = "barcode-scanning", version.ref = "mlkitBarcode" }
```

[`app/build.gradle.kts`](app/build.gradle.kts) le usa:

```kotlin
dependencies {
    implementation(libs.androidx.camera.core)
    implementation(libs.mlkit.barcode.scanning)
}
```

| Concetto Android | Equivalente Python | Equivalente C++ |
|---|---|---|
| `libs.versions.toml` | `requirements.txt` | `vcpkg.json` / `Conan` |
| `implementation(...)` | `pip install ...` | `find_package(...)` in CMake |
| `debugImplementation(...)` | dipendenza solo in sviluppo | libreria linkata solo in debug |
| `testImplementation(...)` | `pytest`, solo nei test | libreria di test |

Quando aggiungi una dipendenza, Gradle la scarica da Maven Central o Google Maven (come PyPI per Python). Il file `gradle/wrapper/gradle-wrapper.properties` fissa la versione di Gradle, come `Pipfile.lock` o `poetry.lock`.

---

## 5. Activity — il punto di ingresso

Un'**Activity** è il contenitore principale di una schermata Android. In StoCatz ne abbiamo solo una ([`MainActivity.kt`](app/src/main/java/com/example/stocatz/MainActivity.kt)) e gestiamo la navigazione internamente con Compose.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()   // estende la UI fino ai bordi dello schermo
        setContent {         // qui inizia il mondo Compose
            StoCatzTheme {
                StoCatzApp()
            }
        }
    }
}
```

**Analogia Python (tkinter):**
```python
root = tk.Tk()
root.title("StoCatz")
# costruisci la UI...
root.mainloop()
```

`onCreate` è come il codice che esegui prima di `mainloop()`. `setContent { }` è dove agganci la tua UI Compose.

### Il ciclo di vita (Lifecycle)

Questo è uno dei concetti più importanti — e più diversi — rispetto allo sviluppo desktop.

```
onCreate → onStart → onResume  ← l'app è in foreground, visibile e attiva
                  ↓
              onPause           ← altra app in foreground (es. chiamata)
                  ↓
              onStop            ← l'app non è più visibile
                  ↓
              onDestroy         ← Android ha distrutto l'Activity (memoria scarsa,
                                   rotazione schermo, back button...)
```

**Perché è importante:** se l'utente ruota il telefono, Android **distrugge e ricrea** l'Activity. Tutto ciò che salvi in variabili normali va perso. Il `ViewModel` esiste proprio per sopravvivere a questo — ne parliamo nella sezione 8.

**Analogia:** in un programma desktop il "ciclo di vita" è semplicemente "aperto / chiuso". Su Android è più complesso perché il SO può sospendere, fermare e distruggere l'app in qualsiasi momento per liberare memoria.

---

## 6. Jetpack Compose — UI dichiarativa

Compose è il moderno toolkit UI di Android. Il concetto chiave: **descrivi come deve apparire la UI dato uno stato, non come aggiornarla passo per passo**.

### Imperativo vs Dichiarativo

```python
# Python (tkinter) — IMPERATIVO: dici al framework cosa fare passo per passo
label = tk.Label(root, text="Nessuna carta")
button = tk.Button(root, text="Aggiungi", command=aggiungi)
label.pack()
button.pack()

# Quando i dati cambiano, aggiorni manualmente:
def aggiorna():
    label.config(text=f"{len(carte)} carte")
```

```kotlin
// Kotlin (Compose) — DICHIARATIVO: descrivi la UI in funzione dei dati
@Composable
fun CardListScreen(cards: List<LoyaltyCard>) {
    if (cards.isEmpty()) {
        Text("Nessuna carta salvata.")
    } else {
        LazyColumn {
            items(cards) { card ->
                Text(card.name)
            }
        }
    }
    // Quando cards cambia, Compose ridisegna AUTOMATICAMENTE
}
```

**Analogia più vicina:** se conosci HTML + JavaScript con React o Vue, Compose funziona in modo simile. Se non li conosci: immagina che invece di dire "vai al widget X e cambia il testo", tu dica "la UI è SEMPRE questa funzione dei dati correnti" e il framework si occupa degli aggiornamenti.

### @Composable — le funzioni come widget

Ogni funzione annotata con `@Composable` è un pezzo di UI. Si combinano come mattoni Lego:

```kotlin
// Una funzione Composable può chiamarne altre
@Composable
fun CardDetailScreen(card: LoyaltyCard, onBack: () -> Unit, ...) {
    Scaffold(                                    // contenitore con TopAppBar + FAB
        topBar = {
            TopAppBar(title = { Text(card.name) })  // barra superiore
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            BarcodeImage(value = card.value, format = card.format)  // custom composable
            Text(card.value)
        }
    }
}
```

**Convenzione di nomenclatura:** le funzioni Composable iniziano con la maiuscola, come le classi in Python/C++. Le funzioni normali in Kotlin iniziano con la minuscola.

### Modifier — la "catena di attributi"

Il `Modifier` si usa per applicare dimensioni, padding, colori di sfondo, eventi di click, ecc. Si incatenano con il punto:

```kotlin
Text(
    text = card.name,
    modifier = Modifier
        .fillMaxWidth()          // larghezza massima disponibile
        .padding(16.dp)          // margine interno di 16 densità-indipendenti
        .background(Color.Blue)  // sfondo blu
        .clickable { onClick() } // reagisce al tap
)
```

**Analogia Python (tkinter):**
```python
label = tk.Label(root, text=card.name, bg="blue", padx=16, pady=16)
label.pack(fill=tk.X)
label.bind("<Button-1>", lambda e: on_click())
```

In tkinter passi tutto nel costruttore del widget o in `pack()`. In Compose il `Modifier` centralizza tutto ciò che riguarda "come si posiziona e si presenta" il widget.

---

## 7. State e Recomposition — il motore di Compose

### Cos'è lo State

Compose "osserva" certe variabili speciali. Quando cambiano, ridisegna automaticamente i Composable che le leggono. Questo processo si chiama **recomposition**.

```kotlin
// In MainActivity.kt:
var screen by remember { mutableStateOf<Screen>(Screen.List) }
```

- `mutableStateOf(...)` crea un contenitore osservabile
- `remember { }` dice a Compose di ricordare questo valore tra una recomposition e l'altra
- `by` è una delega Kotlin: `screen` sembra una variabile normale, ma dietro le quinte usa getter/setter dello State

```kotlin
// Quando assegni un nuovo valore, Compose ridisegna automaticamente
screen = Screen.Detail(card.id)
// ↑ questo innesca una recomposition di tutti i Composable che leggono "screen"
```

**Analogia C++ (con observer pattern):**
```cpp
// In C++ dovresti implementare il pattern observer a mano:
class Screen {
    std::function<void()> on_change;
public:
    void set(ScreenType s) { value = s; on_change(); }
};
```

In Compose tutto questo è automatico.

### remember — sopravvivere alla recomposition

Senza `remember`, ogni recomposition ricrea la variabile da zero:

```kotlin
// SBAGLIATO — name viene resettato a "" ogni volta che Compose ridisegna
@Composable
fun AddCardDialog(...) {
    var name = ""   // ricreato ad ogni recomposition!
}

// CORRETTO
@Composable
fun AddCardDialog(...) {
    var name by remember { mutableStateOf("") }  // persiste tra recomposition
}
```

### DisposableEffect — effetti con cleanup

Per eseguire codice con effetti collaterali (avviare la fotocamera, registrare un listener) si usa `DisposableEffect`. Il blocco `onDispose` viene eseguito quando il Composable lascia la schermata:

```kotlin
// ScannerScreen.kt
DisposableEffect(lifecycleOwner) {
    // SETUP: avvia la fotocamera
    val cameraProvider = cameraProviderFuture.get()
    cameraProvider.bindToLifecycle(lifecycleOwner, ...)

    onDispose {
        // CLEANUP: spegni la fotocamera quando esci dalla schermata
        cameraProvider.unbindAll()
        analysisExecutor.shutdown()
    }
}
```

**Analogia Python:** è il pattern `with` / context manager:
```python
with open("file.txt") as f:
    # setup: il file è aperto
    ...
# cleanup: il file viene chiuso automaticamente all'uscita
```

---

## 8. ViewModel — separare logica dalla UI

Il `ViewModel` vive più a lungo dell'Activity. Sopravvive alla rotazione dello schermo e viene distrutto solo quando l'utente esce definitivamente dall'app.

```
Rotazione schermo:
  Activity (DISTRUTTA) → Activity (RICREATA)
  ViewModel           →   ViewModel           ← lo stesso, con tutti i dati intatti
```

**Regola pratica:** nella UI (Composable) metti solo "come appare". Nel ViewModel metti "cosa fa" e "cosa contiene".

```kotlin
// CardViewModel.kt — logica e dati
class CardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CardRepository(application)
    
    fun addCard(name: String, value: String, ...) {
        viewModelScope.launch { repository.addCard(...) }
    }
    
    fun deleteCard(id: String) {
        viewModelScope.launch { repository.deleteCard(id) }
    }
}

// MainActivity.kt — UI ottiene il ViewModel, non lo crea direttamente
val viewModel: CardViewModel = viewModel()  // Compose gestisce il ciclo di vita
viewModel.deleteCard(card.id)               // chiama l'azione
```

**Analogia Python (MVC):** il ViewModel è il **Controller/Model** in un'architettura MVC. Il Composable è la **View**.

```python
# Python MVC approssimativo
class CardController:
    def __init__(self):
        self.cards = []
    
    def add_card(self, name, value):
        self.cards.append(Card(name=name, value=value))
    
    def delete_card(self, id):
        self.cards = [c for c in self.cards if c.id != id]

# La view chiama il controller, non gestisce i dati direttamente
controller = CardController()
controller.delete_card(card.id)
```

---

## 9. Coroutines — concorrenza senza thread espliciti

Le operazioni lente (I/O su disco, rete, accesso alla fotocamera) non devono bloccare il thread principale — altrimenti l'app si "congela" e Android la chiude con un ANR (Application Not Responding).

In Python gestiresti questo con `asyncio` o i `thread`. In C++ con `std::thread` o `std::future`. In Kotlin si usano le **coroutines**.

```kotlin
// CardViewModel.kt
fun addCard(name: String, value: String, ...) {
    viewModelScope.launch {        // lancia una coroutine nel scope del ViewModel
        repository.addCard(...)    // operazione I/O — NON blocca l'UI
    }
}
```

```python
# Python — equivalente con asyncio
async def add_card(self, name: str, value: str):
    await self.repository.add_card(...)  # I/O asincrono

# o con threading:
threading.Thread(target=self.repository.add_card, args=(name, value)).start()
```

```cpp
// C++ — equivalente con std::async
std::async(std::launch::async, [&]() {
    repository.add_card(name, value);
});
```

### suspend — funzioni che possono essere sospese

Una funzione `suspend` può essere "messa in pausa" in attesa di I/O, senza bloccare il thread:

```kotlin
// CardRepository.kt
suspend fun addCard(card: LoyaltyCard) {
    context.dataStore.edit { prefs ->  // operazione su disco — suspend
        prefs[cardsKey] = json.encodeToString(listSerializer, ...)
    }
}
```

Puoi chiamare una `suspend` function solo da un'altra `suspend` function o da una coroutine (`launch { }`, `async { }`).

**Analogia Python:**
```python
# Le funzioni async in Python corrispondono alle suspend in Kotlin
async def add_card(self, card: LoyaltyCard):
    await self.datastore.edit(...)  # await = può essere sospesa
```

### viewModelScope

`viewModelScope` è uno scope di coroutines legato al ciclo di vita del ViewModel. Quando il ViewModel viene distrutto, tutte le coroutine in quel scope vengono cancellate automaticamente — niente memory leak, niente crash.

```kotlin
fun deleteCard(id: String) {
    viewModelScope.launch {
        repository.deleteCard(id)
        // se il ViewModel viene distrutto mentre aspettiamo, la coroutine viene cancellata
    }
}
```

---

## 10. Flow e StateFlow — stream di dati reattivi

Un `Flow` è uno stream di valori nel tempo. Pensa a un generatore Python che emette valori ogni volta che i dati cambiano:

```python
# Python — generatore come analogia
def cards_stream():
    while True:
        yield read_cards_from_disk()   # emette ogni volta che i dati cambiano
        await asyncio.sleep(...)
```

```kotlin
// Kotlin — Flow
val cards: Flow<List<LoyaltyCard>> = context.dataStore.data.map { prefs ->
    decode(prefs[cardsKey])   // emette ogni volta che DataStore cambia
}
```

### StateFlow — un Flow con un valore "corrente"

`StateFlow` è un Flow speciale che ha sempre un valore corrente accessibile e ricorda l'ultimo valore emesso:

```kotlin
// CardViewModel.kt
private val _searchQuery = MutableStateFlow("")      // privato, modificabile
val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()  // pubblico, read-only

fun setSearchQuery(query: String) {
    _searchQuery.value = query   // modifica il valore, notifica chi ascolta
}
```

**Pattern privato/pubblico:** `_searchQuery` (con underscore, privato) è modificabile. `searchQuery` (senza underscore, pubblico) è solo leggibile dall'esterno. Equivale all'incapsulamento in C++:

```cpp
// C++ equivalente
class CardViewModel {
private:
    std::string search_query_;   // privato, modificabile
public:
    const std::string& search_query() const { return search_query_; }  // pubblico, read-only
    void set_search_query(const std::string& q) { search_query_ = q; }
};
```

### combine — combinare più Flow

```kotlin
// CardViewModel.kt — filtro reattivo: quando cambia la lista O la query, ricalcola
val cards = combine(allCards, _searchQuery) { list, query ->
    if (query.isBlank()) list
    else list.filter { it.name.contains(query, ignoreCase = true) }
}.stateIn(scope = viewModelScope, ...)
```

**Analogia:** è come la programmazione reattiva in Excel — una cella che dipende da altre due si aggiorna automaticamente quando una delle due cambia.

### Collezione nella UI

```kotlin
// MainActivity.kt — la UI "ascolta" il Flow e si aggiorna automaticamente
val cards by viewModel.cards.collectAsStateWithLifecycle()
// "cards" è ora uno State<List<LoyaltyCard>> — ogni nuovo valore innesca una recomposition
```

---

## 11. DataStore — persistenza locale

DataStore è un sostituto moderno delle SharedPreferences. Salva coppie chiave-valore su disco in modo asincrono e thread-safe.

In StoCatz serializziamo l'intera lista di carte in JSON e la salviamo in una singola chiave.

```kotlin
// CardRepository.kt

// Questa riga "attacca" un DataStore al Context dell'app (singleton)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cards")

// Lettura — restituisce un Flow, si aggiorna automaticamente quando il file cambia
val cards: Flow<List<LoyaltyCard>> = context.dataStore.data.map { prefs ->
    decode(prefs[cardsKey])
}

// Scrittura — transazione atomica
suspend fun addCard(card: LoyaltyCard) {
    context.dataStore.edit { prefs ->           // "edit" è una transazione
        val current = decode(prefs[cardsKey])   // leggi il valore attuale
        prefs[cardsKey] = json.encodeToString(listSerializer, current + card)  // scrivi
    }
}
```

**Analogia Python:**
```python
import shelve, json

def add_card(card: dict):
    with shelve.open("cards") as db:       # apre il file (transazione)
        cards = db.get("cards_json", [])   # leggi
        cards.append(card)
        db["cards_json"] = cards           # scrivi

# Per avere reattività in Python dovresti usare un observer o polling:
# threading.Thread(target=lambda: check_for_changes()).start()
```

### Serializzazione con kotlinx.serialization

L'annotazione `@Serializable` su `LoyaltyCard` genera automaticamente il codice per convertire l'oggetto in JSON:

```kotlin
@Serializable
data class LoyaltyCard(val id: String, val name: String, ...)

// Serializzazione
json.encodeToString(listSerializer, listOf(card))
// → '[{"id":"abc","name":"Esselunga","value":"1234567890","format":"EAN_13",...}]'

// Deserializzazione
json.decodeFromString(listSerializer, rawJson)
// → List<LoyaltyCard>
```

**Analogia Python:**
```python
import json
from dataclasses import asdict

# Serializzazione
json.dumps([asdict(card) for card in cards])

# Deserializzazione
[LoyaltyCard(**d) for d in json.loads(raw_json)]
```

---

## 12. CameraX e ML Kit — accesso all'hardware

### Permessi a runtime

Anche se hai dichiarato `CAMERA` nel Manifest, devi chiederlo all'utente la prima volta:

```kotlin
// MainActivity.kt
val cameraPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->                        // callback: "l'utente ha accettato o rifiutato?"
    if (granted) screen = Screen.Scan
}

fun startScan() {
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    if (granted) screen = Screen.Scan
    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)  // mostra il dialog
}
```

**Analogia:** su desktop (Python/C++) di solito non chiedi permessi — accedi direttamente alla webcam con OpenCV. Su Android il SO intermedia ogni accesso all'hardware per proteggere la privacy dell'utente.

### CameraX — pipeline fotocamera

```kotlin
// ScannerScreen.kt
DisposableEffect(lifecycleOwner) {
    val cameraProvider = cameraProviderFuture.get()

    // 1. PREVIEW — mostra ciò che vede la fotocamera sullo schermo
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    // 2. ANALISI — processa ogni frame per trovare codici a barre
    val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // scarta frame vecchi
        .build()
        .also { it.setAnalyzer(analysisExecutor, BarcodeAnalyzer(onBarcodeDetected)) }

    // 3. BIND — attacca tutto al ciclo di vita: la fotocamera si spegne automaticamente
    //    quando esci dalla schermata
    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
        preview, analysis)

    onDispose { cameraProvider.unbindAll() }
}
```

**Analogia Python (OpenCV):**
```python
cap = cv2.VideoCapture(0)
while True:
    ret, frame = cap.read()       # leggi un frame
    result = decode_barcode(frame) # analizza
    if result:
        handle_result(result)
        break
cap.release()                     # cleanup
```

CameraX gestisce il ciclo di vita della fotocamera legandosi all'Activity — come un context manager automatico.

### ML Kit — analisi dei frame

```kotlin
// ScannerScreen.kt — BarcodeAnalyzer
private class BarcodeAnalyzer(
    private val onDetected: (value: String, format: String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private val done = AtomicBoolean(false)  // flag thread-safe per evitare callback multipli

    override fun analyze(imageProxy: ImageProxy) {
        if (done.get()) { imageProxy.close(); return }  // già trovato un barcode, ignora

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.let { barcode ->
                    if (done.compareAndSet(false, true)) {  // primo thread che arriva vince
                        onDetected(barcode.rawValue!!, ...)
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }  // SEMPRE libera il frame
    }
}
```

> ⚠️ `imageProxy.close()` deve sempre essere chiamato — se non lo fai, CameraX smette di mandare frame. È come fare `fclose()` in C o `f.close()` in Python.

`AtomicBoolean` è usato perché `analyze()` viene chiamato da un thread separato per ogni frame. `compareAndSet(false, true)` è un'operazione atomica: imposta il valore a `true` e restituisce `true` solo se era `false` — garantisce che un solo thread esegua `onDetected`.

---

## 13. Architettura complessiva

Ecco come i pezzi si collegano in StoCatz:

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer (Composable functions)                            │
│                                                             │
│  CardListScreen ──── CardDetailScreen ──── ScannerScreen   │
│       │                    │                    │           │
│  AddCardDialog         ColorPicker          BarcodeImage   │
└──────────────────────────────┬──────────────────────────────┘
                               │ legge State, chiama funzioni
┌──────────────────────────────▼──────────────────────────────┐
│  ViewModel Layer                                            │
│                                                             │
│  CardViewModel                                              │
│  ├── cards: StateFlow<List<LoyaltyCard>>   ← filtrata       │
│  ├── allCards: StateFlow<List<LoyaltyCard>> ← completa      │
│  ├── searchQuery: StateFlow<String>                         │
│  ├── addCard() / deleteCard() / updateCardColors()          │
│  └── setSearchQuery()                                       │
└──────────────────────────────┬──────────────────────────────┘
                               │ suspend functions
┌──────────────────────────────▼──────────────────────────────┐
│  Data Layer                                                 │
│                                                             │
│  CardRepository                                             │
│  ├── cards: Flow<List<LoyaltyCard>>   ← da DataStore        │
│  ├── addCard(card)                                          │
│  ├── updateCard(card)                                       │
│  └── deleteCard(id)                                         │
│                                                             │
│  DataStore (su disco) ←→ JSON serialization                 │
└─────────────────────────────────────────────────────────────┘
```

**Il flusso di un'azione tipica — "elimina carta":**

1. L'utente tocca "Elimina" in `CardDetailScreen`
2. Il Composable chiama `onDelete()` (lambda passata da `MainActivity`)
3. `MainActivity` chiama `viewModel.deleteCard(card.id)`
4. Il ViewModel lancia una coroutine: `viewModelScope.launch { repository.deleteCard(id) }`
5. Il Repository scrive su DataStore (operazione `suspend`, su thread I/O)
6. DataStore emette il nuovo valore sul suo `Flow`
7. Il `Flow` nel ViewModel aggiorna `allCards` e `cards`
8. `MainActivity` ha `.collectAsStateWithLifecycle()` su questi Flow → Compose riceve il nuovo State
9. Compose ridisegna `CardListScreen` senza la carta eliminata

Il percorso è sempre **unidirezionale**: UI → ViewModel → Repository → DataStore → Flow → ViewModel → UI. Non ci sono aggiornamenti "a caso" da parti diverse del codice.

---

### Prossimi passi consigliati

| Argomento | Dove esplorare |
|---|---|
| Navigation Compose | Sostituire la `sealed interface Screen` con `androidx.navigation` |
| Room Database | Alternative a DataStore per dati più complessi (è come SQLite con ORM) |
| Dependency Injection | Hilt/Koin per non passare `application` manualmente al Repository |
| Testing | `@Test` con JUnit, `@Composable` testing con `ComposeTestRule` |
| Animazioni | `AnimatedVisibility`, `animateContentSize` in Compose |
