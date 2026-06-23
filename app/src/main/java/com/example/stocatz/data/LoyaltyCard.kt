package com.example.stocatz.data

import kotlinx.serialization.Serializable

/**
 * Una carta fedeltà salvata dall'utente.
 *
 * @param id identificativo univoco della carta
 * @param name nome scelto dall'utente (es. "Esselunga")
 * @param value contenuto del codice a barre (la sequenza di cifre/caratteri letta)
 * @param format formato del codice (nome di [com.google.zxing.BarcodeFormat], es. "EAN_13"),
 *               usato per ridisegnare il codice identico all'originale.
 */
@Serializable
data class LoyaltyCard(
    val id: String,
    val name: String,
    val value: String,
    val format: String
)
