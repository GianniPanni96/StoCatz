package com.example.stocatz.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocatz.data.CardRepository
import com.example.stocatz.data.LoyaltyCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class CardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardRepository(application)

    val cards: StateFlow<List<LoyaltyCard>> = repository.cards.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addCard(name: String, value: String, format: String) {
        viewModelScope.launch {
            repository.addCard(
                LoyaltyCard(
                    id = UUID.randomUUID().toString(),
                    name = name.trim().ifBlank { "Carta" },
                    value = value,
                    format = format
                )
            )
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch { repository.deleteCard(id) }
    }
}
