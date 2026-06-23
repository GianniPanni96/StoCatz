package com.example.stocatz.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cards")

/**
 * Salva e recupera le carte usando DataStore. L'elenco viene serializzato in JSON
 * dentro un'unica chiave: per un portafoglio di carte fedeltà è più che sufficiente.
 */
class CardRepository(private val context: Context) {

    private val cardsKey = stringPreferencesKey("cards_json")
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(LoyaltyCard.serializer())

    val cards: Flow<List<LoyaltyCard>> = context.dataStore.data.map { prefs ->
        decode(prefs[cardsKey])
    }

    suspend fun addCard(card: LoyaltyCard) {
        context.dataStore.edit { prefs ->
            prefs[cardsKey] = json.encodeToString(listSerializer, decode(prefs[cardsKey]) + card)
        }
    }

    suspend fun deleteCard(id: String) {
        context.dataStore.edit { prefs ->
            val updated = decode(prefs[cardsKey]).filterNot { it.id == id }
            prefs[cardsKey] = json.encodeToString(listSerializer, updated)
        }
    }

    private fun decode(raw: String?): List<LoyaltyCard> {
        if (raw == null) return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }.getOrDefault(emptyList())
    }
}
