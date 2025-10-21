package dev.equalparts.glyph_catch.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppBadge
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokemonExpChip
import dev.equalparts.glyph_catch.PokemonLevelChip
import dev.equalparts.glyph_catch.PokemonSpriteCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.EvolutionNotification
import dev.equalparts.glyph_catch.data.InventoryItem
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.gameplay.spawner.Weather
import dev.equalparts.glyph_catch.gameplay.spawner.WeatherProvider
import dev.equalparts.glyph_catch.gameplay.training.TrainingProgression
import dev.equalparts.glyph_catch.ndotFontFamily
import dev.equalparts.glyph_catch.util.ActiveItemStatus
import dev.equalparts.glyph_catch.util.TrainerTipsProvider
import dev.equalparts.glyph_catch.util.rememberActiveItemStatus
import dev.equalparts.glyph_catch.util.rememberSleepBonusStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    db: PokemonDatabase,
    weatherProvider: WeatherProvider,
    tipsProvider: TrainerTipsProvider,
    preferencesManager: PreferencesManager,
    onSettingsClick: () -> Unit,
    onPokemonClick: (CaughtPokemon) -> Unit,
    onPokedexClick: () -> Unit,
    onBagClick: () -> Unit,
    onWeatherSettingsClick: () -> Unit
) {
    val recentCatches by db.pokemonDao().watchRecentCatches(5).collectAsStateWithLifecycle(emptyList())
    val uniqueSpecies by db.pokemonDao().watchPokedexProgress().collectAsStateWithLifecycle(0)
    val glyphToyStatusFlow = remember(preferencesManager) { preferencesManager.watchGlyphToyHasTicked() }
    val glyphToyHasTicked by glyphToyStatusFlow.collectAsStateWithLifecycle(false)
    val totalCaught by db.pokemonDao().watchTotalCaughtCount().collectAsStateWithLifecycle(0)
    val trainingPartner by db.pokemonDao().watchTrainingPartner().collectAsStateWithLifecycle(null)
    val superRodIndicatorFlow = remember(preferencesManager) { preferencesManager.watchSuperRodIndicator() }
    val showSuperRodIndicator by superRodIndicatorFlow.collectAsStateWithLifecycle(
        initialValue = preferencesManager.shouldShowSuperRodIndicator()
    )
    val repelIndicatorFlow = remember(preferencesManager) { preferencesManager.watchRepelIndicator() }
    val showRepelIndicator by repelIndicatorFlow.collectAsStateWithLifecycle(
        initialValue = preferencesManager.shouldShowRepelIndicator()
    )
    val superRodStatus by rememberActiveItemStatus(db, Item.SUPER_ROD)
    val isSleepBonusActive by rememberSleepBonusStatus(preferencesManager)

    val evolutionNotificationsFlow =
        remember(preferencesManager) { preferencesManager.watchPendingEvolutionNotifications() }
    val pendingEvolutionNotifications by evolutionNotificationsFlow.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val currentEvolution = pendingEvolutionNotifications.firstOrNull()

    val progressPercentage = remember(uniqueSpecies) { ((uniqueSpecies * 100) / 151).coerceAtMost(100) }
    val dailyTip = remember(tipsProvider) { tipsProvider.getDailyTip() }
    val weatherFlow = remember(weatherProvider) { weatherProvider.watchWeather() }
    val weather by weatherFlow.collectAsStateWithLifecycle(initialValue = weatherProvider.getCurrentWeather())

    LaunchedEffect(Unit) {
        if (preferencesManager.playerStartDate == 0L) {
            preferencesManager.playerStartDate = System.currentTimeMillis()
        }
        grantStarterStoneGiftIfEligible(db, preferencesManager)
    }

    LaunchedEffect(totalCaught) {
        if (totalCaught >= SUPER_ROD_UNLOCK_COUNT) {
            ensureSuperRodUnlocked(db, preferencesManager)
        }
    }

    LaunchedEffect(uniqueSpecies) {
        if (uniqueSpecies >= REPEL_UNLOCK_SPECIES_COUNT) {
            ensureRepelUnlocked(db, preferencesManager)
        }
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSizes.spacingLarge)
                .verticalScroll(scrollState)
        ) {
            Header(onSettingsClick = onSettingsClick)
            Spacer(modifier = Modifier.height(AppSizes.spacingXLarge))
            if (!glyphToyHasTicked) {
                OnboardingCard()
            } else {
                StatusBanners(
                    showSuperRodIndicator = showSuperRodIndicator,
                    showRepelIndicator = showRepelIndicator,
                    superRodStatus = superRodStatus,
                    isSleepBonusActive = isSleepBonusActive,
                    onSuperRodIndicatorClick = {
                        preferencesManager.markSuperRodIndicatorSeen()
                        onBagClick()
                    },
                    onRepelIndicatorClick = {
                        preferencesManager.markRepelIndicatorSeen()
                        onBagClick()
                    }
                )
                Tiles(
                    progressPercentage = progressPercentage,
                    weather = weather,
                    onPokedexClick = onPokedexClick,
                    onWeatherClick = onWeatherSettingsClick
                )
                Spacer(modifier = Modifier.height(AppSizes.spacingMedium))
                trainingPartner?.let {
                    TrainingBanner(
                        partner = it,
                        onPartnerClick = onPokemonClick
                    )
                }
                Spacer(modifier = Modifier.height(AppSizes.spacingMedium))
                TrainerTipCard(dailyTip = dailyTip)
                Spacer(modifier = Modifier.height(AppSizes.spacingMedium))
                RecentCatches(
                    recentCatches = recentCatches,
                    onPokemonClick = onPokemonClick
                )
            }
        }

        currentEvolution?.let { notification ->
            EvolutionNotificationOverlay(
                notification = notification,
                onDismiss = {
                    scope.launch {
                        preferencesManager.consumeEvolutionNotification()
                    }
                }
            )
        }
    }
}

@Composable
private fun Header(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSizes.spacingMedium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.home_header_title),
            fontFamily = ndotFontFamily,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Normal
        )

        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.nav_settings)
            )
        }
    }
}

@Composable
private fun Tiles(progressPercentage: Int, weather: Weather, onPokedexClick: () -> Unit, onWeatherClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
    ) {
        ProgressCard(
            modifier = Modifier
                .weight(1f)
                .height(AppSizes.homeTileHeight)
                .clickable { onPokedexClick() },
            progressPercentage = progressPercentage
        )

        WeatherCard(
            modifier = Modifier
                .weight(1f)
                .height(AppSizes.homeTileHeight)
                .clickable { onWeatherClick() },
            weather = weather
        )
    }
}

@Composable
private fun ProgressCard(modifier: Modifier = Modifier, progressPercentage: Int) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSizes.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.home_progress_percentage, progressPercentage),
                fontFamily = ndotFontFamily,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.home_progress_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeatherCard(modifier: Modifier = Modifier, weather: Weather) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSizes.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(getWeatherIconResource(weather)),
                contentDescription = stringResource(weather.labelRes()),
                modifier = Modifier.size(AppSizes.homeWeatherImageSize),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(weather.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnboardingCard() {
    val steps = listOf(
        stringResource(R.string.home_onboarding_step_1),
        stringResource(R.string.home_onboarding_step_2),
        stringResource(R.string.home_onboarding_step_3),
        stringResource(R.string.home_onboarding_step_4),
        stringResource(R.string.home_onboarding_step_5),
        stringResource(R.string.home_onboarding_step_6),
        stringResource(R.string.home_onboarding_step_7),
        stringResource(R.string.home_onboarding_step_8),
        stringResource(R.string.home_onboarding_step_9)
    )

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge)
        ) {
            Text(
                text = stringResource(R.string.home_onboarding_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppSizes.spacingSmall))

            Text(
                text = stringResource(R.string.home_onboarding_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppSizes.spacingMedium))

            steps.forEachIndexed { index, step ->
                Text(
                    text = stringResource(R.string.home_onboarding_step_format, index + 1, step),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (index != steps.lastIndex) {
                    Spacer(modifier = Modifier.height(AppSizes.spacingTiny))
                }
            }

            Spacer(modifier = Modifier.height(AppSizes.spacingMedium))

            Text(
                text = stringResource(R.string.home_onboarding_end_1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppSizes.spacingMedium))

            Text(
                text = stringResource(R.string.home_onboarding_end_2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TrainingBanner(partner: CaughtPokemon, onPartnerClick: (CaughtPokemon) -> Unit) {
    val species = Pokemon[partner.speciesId]
    val progress = TrainingProgression.progressFraction(partner.level, partner.exp).coerceIn(0.01f, 1f)

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onPartnerClick(partner) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PokemonSpriteCircle(
                pokemonId = partner.speciesId,
                pokemonName = species?.name ?: stringResource(R.string.common_unknown)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSizes.spacingTiny)
            ) {
                Text(
                    text = partner.nickname ?: species?.name ?: stringResource(R.string.common_unknown),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingTiny)
                ) {
                    AppBadge(text = stringResource(R.string.home_training_badge))
                    PokemonLevelChip(level = partner.level)
                    PokemonExpChip(level = partner.level, exp = partner.exp)
                }
                Spacer(modifier = Modifier.height(AppSizes.spacingMicro))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = { }
                )
            }
        }
    }
}

@Composable
private fun RecentCatches(recentCatches: List<CaughtPokemon>, onPokemonClick: (CaughtPokemon) -> Unit) {
    val referenceTimeMillis = System.currentTimeMillis()
    val newCatches = recentCatches.filter { it.isRecent(referenceTimeMillis) }

    if (newCatches.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.home_empty_recent_catches),
                modifier = Modifier.padding(AppSizes.spacingLarge),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
        ) {
            items(newCatches) { pokemon ->
                RecentCatchCard(
                    pokemon = pokemon,
                    referenceTimeMillis = referenceTimeMillis,
                    onClick = { onPokemonClick(pokemon) }
                )
            }
        }
    }
}

@Composable
private fun TrainerTipCard(dailyTip: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_daily_tip_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(AppSizes.spacingSmall))

                Text(
                    text = dailyTip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EvolutionNotificationOverlay(notification: EvolutionNotification, onDismiss: () -> Unit) {
    val previousSpecies = Pokemon[notification.previousSpeciesId]
    val newSpecies = Pokemon[notification.newSpeciesId]
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.25f },
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSizes.spacingLarge)
            .padding(top = AppSizes.spacingXLarge),
        contentAlignment = Alignment.TopCenter
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {}
        ) {
            AppCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSizes.spacingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium)
                ) {
                    PokemonSpriteCircle(
                        modifier = Modifier.size(AppSizes.homeTileHeight),
                        pokemonId = notification.newSpeciesId,
                        pokemonName = newSpecies?.name ?: stringResource(R.string.common_unknown)
                    )

                    Text(
                        text = stringResource(R.string.home_evolution_congrats_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.home_evolution_congrats_message,
                            previousSpecies?.name ?: stringResource(R.string.common_unknown),
                            newSpecies?.name ?: stringResource(R.string.common_unknown)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.home_evolution_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanners(
    showSuperRodIndicator: Boolean,
    showRepelIndicator: Boolean,
    superRodStatus: ActiveItemStatus,
    isSleepBonusActive: Boolean,
    onSuperRodIndicatorClick: () -> Unit,
    onRepelIndicatorClick: () -> Unit
) {
    if (showSuperRodIndicator || showRepelIndicator || superRodStatus.isActive || isSleepBonusActive) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            if (showSuperRodIndicator) {
                HomeStatusBanner(
                    text = stringResource(R.string.home_super_rod_new_banner),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSuperRodIndicatorClick,
                    highlight = true,
                    badge = stringResource(R.string.home_super_rod_new_badge)
                )
            }
            if (showRepelIndicator) {
                HomeStatusBanner(
                    text = stringResource(R.string.home_repel_new_banner),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRepelIndicatorClick,
                    highlight = true,
                    badge = stringResource(R.string.home_repel_new_badge)
                )
            }
            if (superRodStatus.isActive) {
                HomeStatusBanner(
                    text = stringResource(R.string.home_super_rod_active_banner),
                    modifier = Modifier.fillMaxWidth(),
                    highlight = true,
                    badge = stringResource(R.string.home_super_rod_active_badge, superRodStatus.remainingMinutes)
                )
            }
            if (isSleepBonusActive) {
                HomeStatusBanner(
                    text = stringResource(R.string.home_well_rested_banner),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(AppSizes.spacingMedium))
    }
}

@Composable
private fun HomeStatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlight: Boolean = false,
    badge: String? = null
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        colors = bannerColors(highlight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSizes.spacingLarge,
                    vertical = AppSizes.spacingSmall
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = bannerTextColor(highlight),
                fontWeight = FontWeight.Medium
            )
            if (badge != null) {
                AppBadge(
                    text = badge,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun RecentCatchCard(pokemon: CaughtPokemon, referenceTimeMillis: Long, onClick: () -> Unit) {
    val species = Pokemon[pokemon.speciesId]
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val showNewBadge = remember(pokemon.caughtAt, referenceTimeMillis) {
        pokemon.isRecent(referenceTimeMillis)
    }

    AppCard(
        modifier = Modifier
            .size(AppSizes.homeRecentCardHeight)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSizes.spacingSmall)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PokemonSpriteCircle(
                    modifier = Modifier,
                    pokemonId = pokemon.speciesId,
                    pokemonName = species?.name ?: stringResource(R.string.common_unknown)
                )

                Spacer(modifier = Modifier.height(AppSizes.spacingTiny))

                Text(
                    text = species?.name ?: "???",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = dateFormat.format(Date(pokemon.caughtAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showNewBadge) {
                AppBadge(
                    text = stringResource(R.string.home_recent_new_badge),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
private fun bannerColors(highlight: Boolean) = if (highlight) {
    CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
} else {
    CardDefaults.cardColors()
}

@Composable
private fun bannerTextColor(highlight: Boolean) = if (highlight) {
    MaterialTheme.colorScheme.onPrimary
} else {
    MaterialTheme.colorScheme.onSurface
}

private fun getWeatherIconResource(weather: Weather) = when (weather) {
    Weather.CLEAR -> R.drawable.weather_clear
    Weather.RAIN -> R.drawable.weather_rain
    Weather.THUNDERSTORM -> R.drawable.weather_thunderstorm
    Weather.SNOW -> R.drawable.weather_snow
}

@StringRes
private fun Weather.labelRes(): Int = when (this) {
    Weather.CLEAR -> R.string.home_weather_clear
    Weather.RAIN -> R.string.home_weather_rainy
    Weather.THUNDERSTORM -> R.string.home_weather_storm
    Weather.SNOW -> R.string.home_weather_snowy
}

private suspend fun grantStarterStoneGiftIfEligible(db: PokemonDatabase, preferencesManager: PreferencesManager) {
    if (preferencesManager.hasReceivedStarterStoneGift) {
        return
    }
    val startDate = preferencesManager.playerStartDate
    if (startDate == 0L || startDate > STARTER_STONE_GIFT_CUTOFF_MILLIS) {
        return
    }
    withContext(Dispatchers.IO) {
        val inventoryDao = db.inventoryDao()
        STARTER_STONE_GIFT_ITEMS.forEach { item ->
            val existing = inventoryDao.getItem(item.ordinal)
            if (existing == null) {
                inventoryDao.insertItem(InventoryItem(itemId = item.ordinal, quantity = 1))
            } else {
                inventoryDao.addItems(item.ordinal, 1)
            }
        }
    }
    preferencesManager.hasReceivedStarterStoneGift = true
}

private suspend fun ensureSuperRodUnlocked(db: PokemonDatabase, preferencesManager: PreferencesManager) {
    val created = withContext(Dispatchers.IO) {
        val inventoryDao = db.inventoryDao()
        val existing = inventoryDao.getItem(Item.SUPER_ROD.ordinal)

        if (existing != null) {
            false
        } else {
            inventoryDao.insertItem(InventoryItem(itemId = Item.SUPER_ROD.ordinal, quantity = 1))
            true
        }
    }
    if (created) {
        preferencesManager.markSuperRodDiscovered()
    }
}

private suspend fun ensureRepelUnlocked(db: PokemonDatabase, preferencesManager: PreferencesManager) {
    val created = withContext(Dispatchers.IO) {
        val inventoryDao = db.inventoryDao()
        val existing = inventoryDao.getItem(Item.REPEL.ordinal)

        if (existing != null) {
            false
        } else {
            inventoryDao.insertItem(InventoryItem(itemId = Item.REPEL.ordinal, quantity = 1))
            true
        }
    }
    if (created) {
        preferencesManager.markRepelDiscovered()
    }
}

private const val STARTER_STONE_GIFT_CUTOFF_MILLIS = 1_763_078_400_000L
private val STARTER_STONE_GIFT_ITEMS = listOf(
    Item.FIRE_STONE,
    Item.WATER_STONE,
    Item.LEAF_STONE
)
private const val RECENT_CATCH_WINDOW_MILLIS = 24 * 60 * 60 * 1000L
private const val SUPER_ROD_UNLOCK_COUNT = 15
private const val REPEL_UNLOCK_SPECIES_COUNT = 50

private fun CaughtPokemon.isRecent(referenceTimeMillis: Long): Boolean =
    caughtAt >= referenceTimeMillis - RECENT_CATCH_WINDOW_MILLIS
