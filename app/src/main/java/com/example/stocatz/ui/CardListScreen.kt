package com.example.stocatz.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.stocatz.barcode.BarcodeGenerator
import com.example.stocatz.data.LoyaltyCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    cards: List<LoyaltyCard>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddByScan: () -> Unit,
    onAddByPhoto: () -> Unit,
    onCardClick: (LoyaltyCard) -> Unit
) {
    // Persiste tra rotazioni e process death; non serve DataStore per una preferenza UI
    var isGridView by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Le mie carte") },
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = if (isGridView) "Vista lista" else "Vista griglia"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                FloatingActionButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Aggiungi carta")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Scansiona con fotocamera") },
                        onClick = { menuOpen = false; onAddByScan() }
                    )
                    DropdownMenuItem(
                        text = { Text("Carica da foto o galleria") },
                        onClick = { menuOpen = false; onAddByPhoto() }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Cerca per nome o codice...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancella ricerca")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val bottomPadding = innerPadding.calculateBottomPadding() + 88.dp

            when {
                cards.isEmpty() && searchQuery.isNotBlank() ->
                    NoResults(query = searchQuery, modifier = Modifier.fillMaxSize())

                cards.isEmpty() ->
                    EmptyState(modifier = Modifier.fillMaxSize())

                isGridView -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 4.dp, bottom = bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(cards, key = { it.id }) { card ->
                        CardGridItem(card = card, onClick = { onCardClick(card) })
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        bottom = bottomPadding,
                        start = 16.dp, end = 16.dp, top = 4.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards, key = { it.id }) { card ->
                        CardRow(card = card, onClick = { onCardClick(card) })
                    }
                }
            }
        }
    }
}

// ── Vista lista ───────────────────────────────────────────────────────────────

@Composable
private fun CardRow(card: LoyaltyCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(card.backgroundColor))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color(card.textColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${BarcodeGenerator.humanReadableFormat(card.format)} · ${card.value}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(card.textColor).copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Vista griglia ─────────────────────────────────────────────────────────────

@Composable
private fun CardGridItem(card: LoyaltyCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // proporzioni simili a una carta di credito
            .then(Modifier.padding(0.dp)) // necessario per far rispettare aspectRatio in grid
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(card.backgroundColor))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp)
        ) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color(card.textColor),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = BarcodeGenerator.humanReadableFormat(card.format),
                style = MaterialTheme.typography.labelSmall,
                color = Color(card.textColor).copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(top = 32.dp)
            )
        }
    }
}

// ── Placeholder ───────────────────────────────────────────────────────────────

@Composable
private fun NoResults(query: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "Nessuna carta trovata per \"$query\".",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "Nessuna carta salvata.\nTocca \"+\" per aggiungerne una.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
