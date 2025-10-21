package dev.equalparts.glyph_catch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Pokemon

data class PokemonFilterState(
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val showEventOnly: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || showFavoritesOnly || showEventOnly
}

fun List<CaughtPokemon>.applyFilters(state: PokemonFilterState): List<CaughtPokemon> = filter { pokemon ->
    val species = Pokemon[pokemon.speciesId]
    val matchesSearch = if (state.searchQuery.isBlank()) {
        true
    } else {
        (pokemon.nickname?.contains(state.searchQuery, ignoreCase = true) == true) ||
            (species?.name?.contains(state.searchQuery, ignoreCase = true) == true)
    }
    val matchesFavorite = !state.showFavoritesOnly || pokemon.isFavorite
    val matchesEvent = !state.showEventOnly || pokemon.isSpecialSpawn || pokemon.isConditionalSpawn
    matchesSearch && matchesFavorite && matchesEvent
}.sortedByDescending { it.caughtAt }

@Composable
fun PokemonFilterControls(
    state: PokemonFilterState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        PokemonFilterSearchField(
            query = state.searchQuery,
            onQueryChange = onSearchChange,
            onClearQuery = onClearSearch,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppSizes.spacingSmall))

        PokemonFilterChips(
            showFavoritesOnly = state.showFavoritesOnly,
            onToggleFavorites = onToggleFavorites,
            showEventOnly = state.showEventOnly,
            onToggleEvent = onToggleEvent
        )
    }
}

@Composable
private fun PokemonFilterSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.caught_screen_search_placeholder)) },
        modifier = modifier,
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
private fun PokemonFilterChips(
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
