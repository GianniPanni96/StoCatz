# Regole ProGuard/R8 per StoCatz

# Mantieni le data class serializzate (kotlinx.serialization gestisce le sue regole
# via consumer rules, ma teniamo esplicite le classi dati per sicurezza)
-keep class com.example.stocatz.data.** { *; }

# Kotlin Serialization (già gestito dalla libreria, ma esplicito)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ML Kit – usa le proprie consumer rules, nessuna regola aggiuntiva richiesta

# CameraX – usa le proprie consumer rules, nessuna regola aggiuntiva richiesta

# ZXing – mantieni le classi writer/reader di barcode
-keep class com.google.zxing.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
