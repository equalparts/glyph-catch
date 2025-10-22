package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokemonLevelChip
import dev.equalparts.glyph_catch.PokemonSpriteCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.components.PokemonFilterControls
import dev.equalparts.glyph_catch.components.PokemonFilterState
import dev.equalparts.glyph_catch.components.applyFilters
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.nameRes
import dev.equalparts.glyph_catch.util.canUseItemOn

@Composable
fun PokemonSelectionScreen(
    db: PokemonDatabase,
    item: Item,
    onPokemonSelected: (CaughtPokemon) -> Unit,
    onBackClick: () -> Unit
) {
    val pokemonDao = remember(db) { db.pokemonDao() }
    val caughtPokemon by pokemonDao.watchAllCaught().collectAsStateWithLifecycle(emptyList())

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showEventOnly by remember { mutableStateOf(false) }

    val filterState = PokemonFilterState(
        searchQuery = searchQuery,
        showFavoritesOnly = showFavoritesOnly,
        showEventOnly = showEventOnly
    )

    val allValidTargets by remember(caughtPokemon, item) {
        derivedStateOf { caughtPokemon.filter { canUseItemOn(item, it) } }
    }
    val filteredTargets by remember(allValidTargets, filterState) {
        derivedStateOf { allValidTargets.applyFilters(filterState) }
    }

    AppScaffoldWithTopBar(
        title = stringResource(R.string.pokemon_selection_title),
        onBackClick = onBackClick
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppSizes.spacingLarge, vertical = AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
        ) {
            PokemonFilterControls(
                state = filterState,
                onSearchChange = { searchQuery = it },
                onClearSearch = { searchQuery = "" },
                onToggleFavorites = { showFavoritesOnly = !showFavoritesOnly },
                onToggleEvent = { showEventOnly = !showEventOnly }
            )

            if (allValidTargets.isEmpty()) {
                AppEmptyState(
                    primaryText = stringResource(R.string.pokemon_selection_no_valid_targets),
                    secondaryText = stringResource(item.nameRes())
                )
            } else if (filteredTargets.isEmpty()) {
                AppEmptyState(
                    primaryText = stringResource(R.string.caught_screen_empty_filtered_title),
                    secondaryText = stringResource(R.string.caught_screen_empty_filtered_subtitle)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
                ) {
                    items(
                        items = filteredTargets,
                        key = { it.id }
                    ) { pokemon ->
                        PokemonSelectionRow(
                            pokemon = pokemon,
                            onClick = { onPokemonSelected(pokemon) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonSelectionRow(pokemon: CaughtPokemon, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val species = Pokemon[pokemon.speciesId] ?: return
    val displayName = pokemon.nickname ?: species.name

    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PokemonSpriteCircle(
                pokemonId = pokemon.speciesId,
                pokemonName = species.name,
                modifier = Modifier.size(AppSizes.pokemonImageSize)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingTiny)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                PokemonLevelChip(level = pokemon.level)
            }
        }
    }
}
