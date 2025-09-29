package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokemonLevelChip
import dev.equalparts.glyph_catch.PokemonSpriteCircle
import dev.equalparts.glyph_catch.PokemonTypeChips
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class CaughtPokemonDetailInfo(
    val speciesId: Int?,
    val level: Int,
    val experience: Int,
    val caughtLabel: String,
    val screenOffLabel: String,
    val spawnPoolName: String?,
    val isSpecialSpawn: Boolean,
    val isConditionalSpawn: Boolean
)

@Composable
fun CaughtPokemonDetailScreen(db: PokemonDatabase, pokemonId: String, onNavigateUp: () -> Unit) {
    val pokemonDao = remember(db) { db.pokemonDao() }
    val caughtPokemon by pokemonDao.watchCaughtPokemon(pokemonId).collectAsStateWithLifecycle(null)

    AppScaffoldWithTopBar(
        title = stringResource(R.string.caught_detail_title),
        onBackClick = onNavigateUp
    ) { paddingValues ->
        CaughtPokemonDetailContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSizes.spacingLarge),
            pokemon = caughtPokemon
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CaughtPokemonDetailContent(modifier: Modifier, pokemon: CaughtPokemon?) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        if (pokemon == null) {
            AppEmptyState(
                primaryText = stringResource(R.string.caught_detail_empty_title),
                secondaryText = stringResource(R.string.caught_detail_empty_subtitle)
            )
        } else {
            CaughtPokemonSummaryCard(pokemon = pokemon)

            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.caught_detail_train_button))
            }
        }
    }
}

@Composable
private fun CaughtPokemonSummaryCard(pokemon: CaughtPokemon) {
    val species = Pokemon[pokemon.speciesId]
    val typeLabels = remember(species) {
        buildList {
            species?.let {
                add(it.type1.name)
                it.type2?.let { secondary -> add(secondary.name) }
            }
        }
    }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val caughtAtFormatted = remember(pokemon.caughtAt) { dateFormat.format(Date(pokemon.caughtAt)) }
    val screenOffText = pluralStringResource(
        R.plurals.caught_detail_screen_off_minutes,
        pokemon.screenOffDurationMinutes,
        pokemon.screenOffDurationMinutes
    )

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            CaughtPokemonOverviewRow(pokemon = pokemon, speciesName = species?.name, typeLabels = typeLabels)

            val info = CaughtPokemonDetailInfo(
                speciesId = species?.id,
                level = pokemon.level,
                experience = pokemon.exp,
                caughtLabel = caughtAtFormatted,
                screenOffLabel = screenOffText,
                spawnPoolName = pokemon.spawnPoolName,
                isSpecialSpawn = pokemon.isSpecialSpawn,
                isConditionalSpawn = pokemon.isConditionalSpawn
            )

            CaughtPokemonInfoList(info = info)
        }
    }
}

@Composable
private fun CaughtPokemonOverviewRow(pokemon: CaughtPokemon, speciesName: String?, typeLabels: List<String>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        PokemonSpriteCircle(
            pokemonId = pokemon.speciesId,
            pokemonName = speciesName ?: stringResource(R.string.common_unknown)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            Text(
                text = pokemon.nickname
                    ?: speciesName
                    ?: stringResource(R.string.common_unknown_pokemon),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            PokemonLevelChip(level = pokemon.level)

            if (typeLabels.isNotEmpty()) {
                PokemonTypeChips(types = typeLabels)
            }
        }
    }
}

@Composable
private fun CaughtPokemonInfoList(info: CaughtPokemonDetailInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
        InfoRow(
            label = stringResource(R.string.caught_detail_info_pokedex),
            value = info.speciesId?.let { stringResource(R.string.pokedex_entry_number, it) }
                ?: stringResource(R.string.common_unknown)
        )
        InfoRow(
            label = stringResource(R.string.caught_detail_info_level),
            value = info.level.toString()
        )
        InfoRow(
            label = stringResource(R.string.caught_detail_info_exp),
            value = info.experience.toString()
        )
        InfoRow(
            label = stringResource(R.string.caught_detail_info_caught_on),
            value = info.caughtLabel
        )
        InfoRow(
            label = stringResource(R.string.caught_detail_info_screen_off_time),
            value = info.screenOffLabel
        )

        when {
            info.isSpecialSpawn -> InfoRow(
                label = stringResource(R.string.caught_detail_info_spawn_pool),
                value = stringResource(R.string.caught_detail_encounter_special)
            )

            info.isConditionalSpawn -> InfoRow(
                label = stringResource(R.string.caught_detail_info_spawn_pool),
                value = stringResource(R.string.caught_detail_encounter_event)
            )

            else -> info.spawnPoolName?.let { pool ->
                val formattedPool = pool.replace('_', ' ').replaceFirstChar { char ->
                    char.titlecase(Locale.getDefault())
                }
                InfoRow(
                    label = stringResource(R.string.caught_detail_info_spawn_pool),
                    value = formattedPool
                )
            }
        }
    }
}
