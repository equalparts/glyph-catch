package dev.equalparts.glyph_catch.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppBadge
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSectionHeader
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.PokemonSpriteCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.InventoryItem
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.gameplay.spawner.Weather
import dev.equalparts.glyph_catch.gameplay.spawner.WeatherProvider
import dev.equalparts.glyph_catch.ndotFontFamily
import dev.equalparts.glyph_catch.util.ActiveItemStatus
import dev.equalparts.glyph_catch.util.TrainerTipsProvider
import dev.equalparts.glyph_catch.util.rememberActiveItemStatus
import dev.equalparts.glyph_catch.util.rememberSleepBonusStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
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
    val superRodIndicatorFlow = remember(preferencesManager) { preferencesManager.watchSuperRodIndicator() }
    val showSuperRodIndicator by superRodIndicatorFlow.collectAsStateWithLifecycle(
        initialValue = preferencesManager.shouldShowSuperRodIndicator()
    )
    val superRodStatus by rememberActiveItemStatus(db, Item.SUPER_ROD)
    val isSleepBonusActive by rememberSleepBonusStatus(preferencesManager)

    val progressPercentage = remember(uniqueSpecies) { ((uniqueSpecies * 100) / 151).coerceAtMost(100) }
    val dailyTip = remember(tipsProvider) { tipsProvider.getDailyTip() }
    val weatherFlow = remember(weatherProvider) { weatherProvider.watchWeather() }
    val weather by weatherFlow.collectAsStateWithLifecycle(initialValue = weatherProvider.getCurrentWeather())

    LaunchedEffect(Unit) {
        if (preferencesManager.playerStartDate == 0L) {
            preferencesManager.playerStartDate = System.currentTimeMillis()
        }
    }

    LaunchedEffect(totalCaught) {
        if (totalCaught >= SUPER_ROD_UNLOCK_COUNT) {
            ensureSuperRodUnlocked(db, preferencesManager)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSizes.spacingLarge)
            .verticalScroll(scrollState)
    ) {
        Header(onSettingsClick = onSettingsClick)
        Spacer(modifier = Modifier.height(AppSizes.spacingXLarge))
        StatusBanners(
            showSuperRodIndicator = showSuperRodIndicator,
            superRodStatus = superRodStatus,
            isSleepBonusActive = isSleepBonusActive,
            onSuperRodIndicatorClick = {
                preferencesManager.markSuperRodIndicatorSeen()
                onBagClick()
            }
        )
        if (!glyphToyHasTicked) {
            OnboardingCard()
        } else {
            Tiles(
                progressPercentage = progressPercentage,
                weather = weather,
                onPokedexClick = onPokedexClick,
                onWeatherClick = onWeatherSettingsClick
            )
            Spacer(modifier = Modifier.height(AppSizes.spacingMedium))
            TrainerTipCard(dailyTip = dailyTip)
            Spacer(modifier = Modifier.height(AppSizes.spacingMedium))
            RecentCatches(
                recentCatches = recentCatches,
                onPokemonClick = onPokemonClick
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
private fun RecentCatches(recentCatches: List<CaughtPokemon>, onPokemonClick: (CaughtPokemon) -> Unit) {
    AppSectionHeader(text = stringResource(R.string.home_section_recent_catches))
    if (recentCatches.isEmpty()) {
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
            items(recentCatches) { pokemon ->
                RecentCatchCard(
                    pokemon = pokemon,
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
private fun StatusBanners(
    showSuperRodIndicator: Boolean,
    superRodStatus: ActiveItemStatus,
    isSleepBonusActive: Boolean,
    onSuperRodIndicatorClick: () -> Unit
) {
    if (showSuperRodIndicator || superRodStatus.isActive || isSleepBonusActive) {
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
                AppBadge(text = badge)
            }
        }
    }
}

@Composable
private fun RecentCatchCard(pokemon: CaughtPokemon, onClick: () -> Unit) {
    val species = Pokemon[pokemon.speciesId]
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    AppCard(
        modifier = Modifier
            .size(AppSizes.homeRecentCardHeight)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSizes.spacingSmall),
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

private suspend fun ensureSuperRodUnlocked(db: PokemonDatabase, preferencesManager: PreferencesManager) {
    withContext(Dispatchers.IO) {
        val inventoryDao = db.inventoryDao()
        val existing = inventoryDao.getItem(Item.SUPER_ROD.ordinal)

        when {
            existing == null -> inventoryDao.insertItem(
                InventoryItem(itemId = Item.SUPER_ROD.ordinal, quantity = 1)
            )
            existing.quantity <= 0 -> inventoryDao.addItems(
                Item.SUPER_ROD.ordinal,
                1 - existing.quantity
            )
        }

        if (!preferencesManager.hasDiscoveredSuperRod) {
            preferencesManager.markSuperRodDiscovered()
        }
    }
}

private const val SUPER_ROD_UNLOCK_COUNT = 15
