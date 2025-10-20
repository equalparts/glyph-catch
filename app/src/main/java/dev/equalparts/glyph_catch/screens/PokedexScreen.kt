package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppScreenHeader
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokedexSpriteCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.ndotFontFamily

private data class PokedexScreenState(val progress: Int, val caughtSpeciesIds: Set<Int>)

@Composable
fun PokedexScreen(db: PokemonDatabase, onPokemonClick: (Int) -> Unit = {}) {
    val progress by db.pokemonDao().watchPokedexProgress().collectAsStateWithLifecycle(0)
    val caughtSpeciesIds by db.pokemonDao().watchCaughtSpeciesIds().collectAsStateWithLifecycle(emptyList())
    val caughtSpeciesSet = remember(caughtSpeciesIds) {
        caughtSpeciesIds.toSet()
    }

    val state = PokedexScreenState(progress = progress, caughtSpeciesIds = caughtSpeciesSet)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSizes.spacingXLarge)
    ) {
        AppScreenHeader(
            title = stringResource(R.string.pokedex_title),
            subtitle = stringResource(R.string.pokedex_progress_subtitle, state.progress)
        )

        Spacer(modifier = Modifier.height(AppSizes.spacingLarge))
        PokedexGrid(state = state, onPokemonClick = onPokemonClick)
    }
}

@Composable
private fun PokedexGrid(state: PokedexScreenState, onPokemonClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        items(count = 151) { index ->
            val pokemonId = index + 1
            val caught = pokemonId in state.caughtSpeciesIds
            val species = Pokemon.all[pokemonId] ?: return@items

            PokedexEntryCard(
                pokemonId = pokemonId,
                species = species,
                caught = caught,
                onClick = if (caught) {
                    { onPokemonClick(pokemonId) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun PokedexEntryCard(pokemonId: Int, species: PokemonSpecies, caught: Boolean, onClick: (() -> Unit)?) {
    val clickModifier = if (caught && onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    AppCard(
        modifier = Modifier
            .aspectRatio(0.85f)
            .then(clickModifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSizes.spacingExtraSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PokedexSpriteCircle(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f),
                pokemonId = pokemonId,
                pokemonName = species.name,
                caught = caught,
                size = null
            )

            Spacer(modifier = Modifier.height(AppSizes.spacingMicro))

            PokedexEntryLabel(species = species, caught = caught)

            Text(
                text = stringResource(R.string.pokedex_entry_number, pokemonId),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PokedexEntryLabel(species: PokemonSpecies, caught: Boolean) {
    if (caught) {
        val fontSize = when {
            species.name.length >= 10 -> MaterialTheme.typography.labelMedium.fontSize
            else -> MaterialTheme.typography.labelLarge.fontSize
        }
        Text(
            text = species.name,
            fontFamily = ndotFontFamily,
            fontSize = fontSize,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    } else {
        Text(
            text = "???",
            fontFamily = ndotFontFamily,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
