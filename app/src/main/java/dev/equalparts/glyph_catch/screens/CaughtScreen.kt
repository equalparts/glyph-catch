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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableChipColors
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private data class CaughtUiState(
    val totalCaught: Int,
    val searchQuery: String,
    val showFavoritesOnly: Boolean,
    val showEventOnly: Boolean,
    val hasActiveFilters: Boolean,
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

    val hasActiveFilters = searchQuery.isNotBlank() || showFavoritesOnly || showEventOnly

    val filteredPokemon by remember(caughtPokemon, searchQuery, showFavoritesOnly, showEventOnly) {
        derivedStateOf {
            caughtPokemon
                .filter { pokemon ->
                    val species = Pokemon[pokemon.speciesId]
                    val matchesSearch = if (searchQuery.isBlank()) {
                        true
                    } else {
                        (pokemon.nickname?.contains(searchQuery, ignoreCase = true) == true) ||
                            (species?.name?.contains(searchQuery, ignoreCase = true) == true)
                    }
                    val matchesFavorite = !showFavoritesOnly || pokemon.isFavorite
                    val matchesEvent = !showEventOnly || pokemon.isSpecialSpawn || pokemon.isConditionalSpawn
                    matchesSearch && matchesFavorite && matchesEvent
                }
                .sortedByDescending { it.caughtAt }
        }
    }

    val state = CaughtUiState(
        totalCaught = totalCaught,
        searchQuery = searchQuery,
        showFavoritesOnly = showFavoritesOnly,
        showEventOnly = showEventOnly,
        hasActiveFilters = hasActiveFilters,
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

        CaughtSearchSection(state = state, actions = actions)

        Spacer(modifier = Modifier.height(AppSizes.spacingLarge))

        CaughtResults(state = state, actions = actions)
    }
}

@Composable
private fun CaughtSearchSection(state: CaughtUiState, actions: CaughtActions) {
    Column {
        CaughtSearchField(
            query = state.searchQuery,
            onQueryChange = actions.onSearchChange,
            onClearQuery = actions.onClearSearch
        )

        Spacer(modifier = Modifier.height(AppSizes.spacingSmall))

        CaughtFilterRow(
            showFavoritesOnly = state.showFavoritesOnly,
            onToggleFavorites = actions.onToggleFavorites,
            showEventOnly = state.showEventOnly,
            onToggleEvent = actions.onToggleEvent
        )
    }
}

@Composable
private fun CaughtResults(state: CaughtUiState, actions: CaughtActions) {
    if (state.caughtPokemon.isEmpty()) {
        AppEmptyState(
            primaryText = if (state.hasActiveFilters) {
                stringResource(R.string.caught_screen_empty_filtered_title)
            } else {
                stringResource(R.string.caught_screen_empty_title)
            },
            secondaryText = if (state.hasActiveFilters) {
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
private fun CaughtSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.caught_screen_search_placeholder)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(AppSizes.cardCornerRadius),
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.caught_screen_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            null
        }
    )
}

@Composable
private fun CaughtFilterRow(
    showFavoritesOnly: Boolean,
    onToggleFavorites: () -> Unit,
    showEventOnly: Boolean,
    onToggleEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
    ) {
        ToggleFilterChip(
            label = stringResource(R.string.caught_screen_filter_favorites),
            icon = Icons.Default.Favorite,
            selected = showFavoritesOnly,
            onClick = onToggleFavorites,
            colors = chipColors
        )

        ToggleFilterChip(
            label = stringResource(R.string.caught_screen_filter_event),
            icon = Icons.Default.Star,
            selected = showEventOnly,
            onClick = onToggleEvent,
            colors = chipColors
        )
    }
}

@Composable
private fun ToggleFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    colors: SelectableChipColors,
    modifier: Modifier = Modifier
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AppSizes.iconSizeSmall)
            )
        },
        colors = colors
    )
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
    val typeLabels = remember(species) {
        buildList {
            add(species.type1.name)
            species.type2?.let { add(it.name) }
        }
    }

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
