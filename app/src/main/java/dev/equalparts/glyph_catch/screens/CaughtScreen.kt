package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScreenHeader
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokemonExpChip
import dev.equalparts.glyph_catch.PokemonLevelChip
import dev.equalparts.glyph_catch.PokemonSpriteCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.components.PokemonFilterControls
import dev.equalparts.glyph_catch.components.PokemonFilterState
import dev.equalparts.glyph_catch.components.applyFilters
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private data class CaughtUiState(
    val totalCaught: Int,
    val filterState: PokemonFilterState,
    val caughtPokemon: List<CaughtPokemon>
)

private data class CaughtActions(
    val onSearchChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onToggleFavorites: () -> Unit,
    val onToggleEvent: () -> Unit,
    val onToggleFavorite: (CaughtPokemon) -> Unit,
    val onPokemonClick: (CaughtPokemon) -> Unit
)

@Composable
fun CaughtScreen(db: PokemonDatabase, initialSearchQuery: String = "", onPokemonClick: (CaughtPokemon) -> Unit = {}) {
    val pokemonDao = remember(db) { db.pokemonDao() }
    val caughtPokemon by pokemonDao.watchAllCaught().collectAsStateWithLifecycle(emptyList())
    val totalCaught by pokemonDao.watchTotalCaughtCount().collectAsStateWithLifecycle(0)
    val scope = rememberCoroutineScope()

    var searchQuery by rememberSaveable(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showEventOnly by remember { mutableStateOf(false) }

    val toggleFavorite: (CaughtPokemon) -> Unit = { pokemon ->
        scope.launch { pokemonDao.updateFavorite(pokemon.id, !pokemon.isFavorite) }
    }

    val filterState = PokemonFilterState(
        searchQuery = searchQuery,
        showFavoritesOnly = showFavoritesOnly,
        showEventOnly = showEventOnly
    )

    val filteredPokemon by remember(caughtPokemon, filterState) {
        derivedStateOf { caughtPokemon.applyFilters(filterState) }
    }

    val state = CaughtUiState(
        totalCaught = totalCaught,
        filterState = filterState,
        caughtPokemon = filteredPokemon
    )

    val actions = CaughtActions(
        onSearchChange = { searchQuery = it },
        onClearSearch = { searchQuery = "" },
        onToggleFavorites = { showFavoritesOnly = !showFavoritesOnly },
        onToggleEvent = { showEventOnly = !showEventOnly },
        onToggleFavorite = toggleFavorite,
        onPokemonClick = onPokemonClick
    )

    CaughtScreenContent(state = state, actions = actions)
}

@Composable
private fun CaughtScreenContent(state: CaughtUiState, actions: CaughtActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSizes.spacingXLarge)
    ) {
        AppScreenHeader(
            title = stringResource(R.string.caught_screen_title),
            subtitle = stringResource(R.string.caught_screen_total_caught, state.totalCaught)
        )

        PokemonFilterControls(
            state = state.filterState,
            onSearchChange = actions.onSearchChange,
            onClearSearch = actions.onClearSearch,
            onToggleFavorites = actions.onToggleFavorites,
            onToggleEvent = actions.onToggleEvent
        )

        Spacer(modifier = Modifier.height(AppSizes.spacingLarge))

        CaughtResults(state = state, actions = actions)
    }
}

@Composable
private fun CaughtResults(state: CaughtUiState, actions: CaughtActions) {
    if (state.caughtPokemon.isEmpty()) {
        AppEmptyState(
            primaryText = if (state.filterState.hasActiveFilters) {
                stringResource(R.string.caught_screen_empty_filtered_title)
            } else {
                stringResource(R.string.caught_screen_empty_title)
            },
            secondaryText = if (state.filterState.hasActiveFilters) {
                stringResource(R.string.caught_screen_empty_filtered_subtitle)
            } else {
                null
            }
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
        ) {
            items(
                items = state.caughtPokemon,
                key = { it.id }
            ) { pokemon ->
                CaughtPokemonCard(
                    pokemon = pokemon,
                    onToggleFavorite = actions.onToggleFavorite,
                    onClick = actions.onPokemonClick
                )
            }
        }
    }
}

@Composable
fun CaughtPokemonCard(
    pokemon: CaughtPokemon,
    onToggleFavorite: (CaughtPokemon) -> Unit,
    onClick: (CaughtPokemon) -> Unit
) {
    val species = Pokemon[pokemon.speciesId]!!
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(pokemon.caughtAt) { dateFormat.format(Date(pokemon.caughtAt)) }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(pokemon) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PokemonSpriteCircle(
                pokemonId = pokemon.speciesId,
                pokemonName = species.name
            )

            Spacer(modifier = Modifier.size(AppSizes.spacingMedium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pokemon.nickname ?: species.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(AppSizes.spacingTiny))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
                ) {
                    PokemonLevelChip(level = pokemon.level)
                    PokemonExpChip(level = pokemon.level, exp = pokemon.exp)
                }

                Spacer(modifier = Modifier.height(AppSizes.spacingTiny))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (pokemon.isSpecialSpawn || pokemon.isConditionalSpawn) {
                        Text(
                            text = "★",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = { onToggleFavorite(pokemon) }) {
                Icon(
                    imageVector = if (pokemon.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.caught_screen_favorite_content_description),
                    tint = if (pokemon.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
