package dev.equalparts.glyph_catch.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokemonExpChip
import dev.equalparts.glyph_catch.PokemonLevelChip
import dev.equalparts.glyph_catch.PokemonSpriteCircle
import dev.equalparts.glyph_catch.PokemonTypeChips
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDao
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.util.useItemOnPokemon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private data class CaughtPokemonDetailInfo(
    val speciesId: Int?,
    val level: Int,
    val experience: Int,
    val gender: dev.equalparts.glyph_catch.data.Gender,
    val happiness: Int,
    val steps: Int,
    val requiredSteps: Int,
    val eggGroups: List<dev.equalparts.glyph_catch.data.EggGroup>,
    val appearedLabel: String?,
    val caughtLabel: String,
    val screenOffLabel: String,
    val spawnPoolName: String?,
    val isSpecialSpawn: Boolean,
    val isConditionalSpawn: Boolean
)

@Composable
fun CaughtPokemonDetailScreen(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    pokemonId: String,
    onNavigateUp: () -> Unit
) {
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
            pokemon = caughtPokemon,
            pokemonDao = pokemonDao,
            preferencesManager = preferencesManager,
            db = db
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
private fun CaughtPokemonDetailContent(
    modifier: Modifier,
    pokemon: CaughtPokemon?,
    pokemonDao: PokemonDao,
    preferencesManager: PreferencesManager,
    db: PokemonDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isStartingTraining by remember { mutableStateOf(false) }
    var showSlotSelection by remember { mutableStateOf(false) }
    val trainingPartners by pokemonDao.watchTrainingPartners().collectAsStateWithLifecycle(emptyList())
    val activeEggId by preferencesManager.watchActiveEggId().collectAsStateWithLifecycle(null)
    val cookieItem by db.inventoryDao().watchItem(Item.SOOTHE_BELL_COOKIE.ordinal).collectAsStateWithLifecycle(null)
    val cookieCount = cookieItem?.quantity ?: 0
    var showCookieConfirmation by remember { mutableStateOf(false) }
    var isUsingCookie by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pokemon != null) {
            preferencesManager.activeEggId = pokemon.id
        }
    }

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

            if (pokemon.isEgg) {
                val isInPouch = activeEggId == pokemon.id
                Button(
                    onClick = {
                        val permission = Manifest.permission.ACTIVITY_RECOGNITION
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            preferencesManager.activeEggId = pokemon.id
                        } else {
                            permissionLauncher.launch(permission)
                        }
                    },
                    enabled = !isInPouch,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isInPouch) {
                            stringResource(R.string.caught_detail_in_egg_pouch)
                        } else {
                            stringResource(R.string.caught_detail_put_in_egg_pouch)
                        }
                    )
                }

                if (isInPouch) {
                    OutlinedButton(
                        onClick = {
                            preferencesManager.activeEggId = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.common_cancel))
                    }
                }
            }

            if (!pokemon.isEgg) {
                Button(
                    onClick = {
                        showSlotSelection = true
                    },
                    enabled = !pokemon.isTraining && !isStartingTraining,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (pokemon.isTraining) {
                            stringResource(R.string.caught_detail_is_training)
                        } else {
                            stringResource(R.string.caught_detail_start_training)
                        }
                    )
                }
                if (pokemon.isTraining) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pokemonDao.stopTraining(pokemon.id)
                                preferencesManager.clearTrainingPartner(pokemon.trainingSlot)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.caught_detail_stop_training))
                    }
                }

                OutlinedButton(
                    onClick = { showCookieConfirmation = true },
                    enabled = cookieCount > 0 && !isUsingCookie && pokemon.happiness < 255,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = stringResource(R.string.item_usage_give_cookie_count, cookieCount)
                    Text(text = label)
                }
            }
        }
    }

    if (showCookieConfirmation && pokemon != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCookieConfirmation = false },
            title = {
                Text(
                    text = stringResource(
                        R.string.item_usage_confirm_title,
                        stringResource(R.string.item_soothe_bell_cookie_name)
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.item_usage_confirm_message,
                        stringResource(R.string.item_soothe_bell_cookie_name),
                        pokemon.nickname ?: Pokemon[pokemon.speciesId]?.name
                        ?: stringResource(R.string.common_unknown)
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showCookieConfirmation = false
                        scope.launch {
                            isUsingCookie = true
                            try {
                                useItemOnPokemon(
                                    db,
                                    preferencesManager,
                                    Item.SOOTHE_BELL_COOKIE,
                                    pokemon.id
                                )
                            } finally {
                                isUsingCookie = false
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.item_usage_confirm_button))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showCookieConfirmation = false
                }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showSlotSelection && pokemon != null) {
        TrainingSlotSelectionDialog(
            partners = trainingPartners,
            onSlotSelected = { slot ->
                scope.launch {
                    isStartingTraining = true
                    try {
                        pokemonDao.setActiveTrainingPartner(pokemon.id, slot)
                        preferencesManager.markTrainingPartner(slot, pokemon.id)
                        showSlotSelection = false
                    } finally {
                        isStartingTraining = false
                    }
                }
            },
            onDismiss = { showSlotSelection = false }
        )
    }
}

@Composable
private fun TrainingSlotSelectionDialog(
    partners: List<CaughtPokemon>,
    onSlotSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.training_slot_selection_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)) {
                for (slot in 1..2) {
                    val occupant = partners.find { it.trainingSlot == slot }
                    val occupantSpecies = occupant?.let { Pokemon[it.speciesId] }
                    val label = if (occupant != null) {
                        stringResource(
                            R.string.training_slot_label,
                            slot,
                            occupant.nickname ?: occupantSpecies?.name ?: stringResource(R.string.common_unknown),
                            occupant.level
                        )
                    } else {
                        stringResource(R.string.training_slot_empty, slot)
                    }

                    Button(
                        onClick = { onSlotSelected(slot) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        }
    )
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
    val appearedAtFormatted = remember(pokemon.spawnedAt) {
        pokemon.spawnedAt.takeIf { it > 0L }?.let { Date(it) }?.let(dateFormat::format)
    }
    val caughtAtFormatted = remember(pokemon.caughtAt) { dateFormat.format(Date(pokemon.caughtAt)) }
    val screenOffText = pluralStringResource(
        R.plurals.caught_detail_screen_off_minutes,
        pokemon.screenOffDurationMinutes,
        pokemon.screenOffDurationMinutes
    )

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CaughtPokemonOverview(
                pokemon = pokemon,
                speciesName = species?.name,
                typeLabels = typeLabels
            )

            val info = CaughtPokemonDetailInfo(
                speciesId = species?.id,
                level = pokemon.level,
                experience = pokemon.exp,
                gender = pokemon.gender,
                happiness = pokemon.happiness,
                steps = pokemon.steps,
                requiredSteps = pokemon.requiredSteps,
                eggGroups = species?.eggGroups ?: emptyList(),
                appearedLabel = appearedAtFormatted,
                caughtLabel = caughtAtFormatted,
                screenOffLabel = screenOffText,
                spawnPoolName = pokemon.spawnPoolName,
                isSpecialSpawn = pokemon.isSpecialSpawn,
                isConditionalSpawn = pokemon.isConditionalSpawn
            )

            CaughtPokemonInfoList(info = info, isEgg = pokemon.isEgg)
        }
    }
}

@Composable
private fun CaughtPokemonOverview(pokemon: CaughtPokemon, speciesName: String?, typeLabels: List<String>) {
    val name = if (pokemon.isEgg) {
        stringResource(R.string.caught_pokemon_egg_name)
    } else {
        val baseName = pokemon.nickname ?: speciesName ?: stringResource(R.string.common_unknown)
        if (pokemon.variant != null) {
            "$baseName ${pokemon.variant.uppercase()}"
        } else {
            baseName
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
    ) {
        if (pokemon.isEgg) {
            PokemonSpriteCircle(
                modifier = Modifier.size(AppSizes.homeTileHeight),
                painter = androidx.compose.ui.res.painterResource(R.drawable.matrix_egg),
                contentDescription = name
            )
        } else {
            PokemonSpriteCircle(
                modifier = Modifier.size(AppSizes.homeTileHeight),
                pokemonId = pokemon.speciesId,
                pokemonName = speciesName ?: stringResource(R.string.common_unknown),
                variant = pokemon.variant
            )
        }

        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (pokemon.isEgg) {
            val progress = if (pokemon.requiredSteps > 0) {
                pokemon.steps.toFloat() / pokemon.requiredSteps
            } else {
                0f
            }
            val descriptionRes = when {
                progress < 0.25f -> R.string.caught_pokemon_egg_description_1
                progress < 0.50f -> R.string.caught_pokemon_egg_description_2
                progress < 0.75f -> R.string.caught_pokemon_egg_description_3
                else -> R.string.caught_pokemon_egg_description_4
            }
            Text(
                text = stringResource(descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingTiny)
            ) {
                PokemonLevelChip(level = pokemon.level)
                PokemonExpChip(level = pokemon.level, exp = pokemon.exp)
            }

            if (typeLabels.isNotEmpty()) {
                PokemonTypeChips(
                    types = typeLabels,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun CaughtPokemonInfoList(info: CaughtPokemonDetailInfo, isEgg: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)) {
        if (!isEgg) {
            InfoRow(
                label = stringResource(R.string.caught_detail_info_pokedex),
                value = info.speciesId?.let { stringResource(R.string.pokedex_entry_number, it) }
                    ?: stringResource(R.string.common_unknown)
            )
            InfoRow(
                label = stringResource(R.string.caught_detail_info_gender),
                value = when (info.gender) {
                    dev.equalparts.glyph_catch.data.Gender.MALE -> "Male ♂"
                    dev.equalparts.glyph_catch.data.Gender.FEMALE -> "Female ♀"
                    dev.equalparts.glyph_catch.data.Gender.GENDERLESS -> "Genderless"
                }
            )
            InfoRow(
                label = stringResource(R.string.caught_detail_info_happiness),
                value = "${info.happiness}/255"
            )
            if (info.eggGroups.isNotEmpty()) {
                InfoRow(
                    label = stringResource(R.string.caught_detail_info_egg_groups),
                    value = info.eggGroups.joinToString { group ->
                        group.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                    }
                )
            }

        } else {
            val stepsValue = if (info.requiredSteps > 0) {
                "${info.steps}/${info.requiredSteps}"
            } else {
                info.steps.toString()
            }
            InfoRow(
                label = stringResource(R.string.caught_detail_info_steps),
                value = stepsValue
            )
        }

        InfoRow(
            label = stringResource(R.string.caught_detail_info_appeared_on),
            value = info.appearedLabel ?: stringResource(R.string.common_unknown)
        )
        InfoRow(
            label = stringResource(R.string.caught_detail_info_caught_on),
            value = info.caughtLabel
        )

        if (!isEgg) {
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
}
