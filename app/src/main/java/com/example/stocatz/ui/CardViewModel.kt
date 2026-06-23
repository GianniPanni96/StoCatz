package com.example.stocatz.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocatz.data.CardRepository
import com.example.stocatz.data.LoyaltyCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class CardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allCards: StateFlow<List<LoyaltyCard>> = repository.cards.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val cards: StateFlow<List<LoyaltyCard>> = combine(allCards, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.value.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun addCard(name: String, value: String, format: String, backgroundColor: Int, textColor: Int) {
        viewModelScope.launch {
            repository.addCard(
                LoyaltyCard(
                    id = UUID.randomUUID().toString(),
                    name = name.trim().ifBlank { "Carta" },
                    value = value,
                    format = format,
                    backgroundColor = backgroundColor,
                    textColor = textColor
                )
            )
        }
    }

    fun updateCardColors(id: String, backgroundColor: Int, textColor: Int) {
        viewModelScope.launch {
            val card = allCards.value.firstOrNull { it.id == id } ?: return@launch
            repository.updateCard(card.copy(backgroundColor = backgroundColor, textColor = textColor))
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch { repository.deleteCard(id) }
    }
}
