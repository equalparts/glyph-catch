package dev.equalparts.glyph_catch.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.gameplay.WeatherProviderFactory
import dev.equalparts.glyph_catch.screens.CaughtPokemonDetailScreen
import dev.equalparts.glyph_catch.screens.CaughtScreen
import dev.equalparts.glyph_catch.screens.HomeScreen
import dev.equalparts.glyph_catch.screens.InventoryScreen
import dev.equalparts.glyph_catch.screens.PokedexScreen
import dev.equalparts.glyph_catch.screens.PokemonSelectionScreen
import dev.equalparts.glyph_catch.screens.SettingsScreen
import dev.equalparts.glyph_catch.screens.inventory.InventoryItemDetailScreen
import dev.equalparts.glyph_catch.util.TrainerTipsProvider

@Composable
fun AppNavigationGraph(navController: NavHostController, db: PokemonDatabase) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val weatherConfigFlow = remember(preferencesManager) { preferencesManager.watchWeatherConfig() }
    val weatherConfig by weatherConfigFlow.collectAsStateWithLifecycle(preferencesManager.getWeatherConfig())
    val weatherProvider = remember(weatherConfig) {
        WeatherProviderFactory.create(preferencesManager, weatherConfig)
    }
    val tipsProvider = remember { TrainerTipsProvider(context, preferencesManager) }

    NavHost(
        navController = navController,
        startDestination = AppScreen.Home.route
    ) {
        composable(AppScreen.Home.route) {
            HomeScreen(
                db = db,
                weatherProvider = weatherProvider,
                tipsProvider = tipsProvider,
                preferencesManager = preferencesManager,
                onSettingsClick = {
                    navController.navigate(AppScreen.Settings.route)
                },
                onPokemonClick = { caughtPokemon ->
                    navController.navigateToCaughtDetail(caughtPokemon.id, fromHome = true)
                },
                onPokedexClick = {
                    navController.navigate(AppScreen.Pokedex.route)
                },
                onBagClick = {
                    navController.navigate(AppScreen.Inventory.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onWeatherSettingsClick = {
                    navController.navigate(AppScreen.Settings.route)
                }
            )
        }
        composable(AppScreen.Pokedex.route) {
            PokedexScreen(
                db = db,
                onPokemonClick = { speciesId ->
                    navController.navigateToPokemon(speciesId)
                }
            )
        }
        composable(
            route = AppScreen.Caught.route,
            arguments = listOf(
                navArgument("search") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val search = backStackEntry.arguments?.getString("search") ?: ""
            CaughtScreen(
                db = db,
                initialSearchQuery = search,
                onPokemonClick = { caughtPokemon ->
                    navController.navigateToCaughtDetail(caughtPokemon.id)
                }
            )
        }
        listOf("caught/detail/{pokemonId}", "home/detail/{pokemonId}").forEach { route ->
            composable(
                route = route,
                arguments = listOf(
                    navArgument("pokemonId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val pokemonId = backStackEntry.arguments?.getString("pokemonId") ?: return@composable
                CaughtPokemonDetailScreen(
                    db = db,
                    preferencesManager = preferencesManager,
                    pokemonId = pokemonId,
                    onNavigateUp = { navController.navigateUp() }
                )
            }
        }

        composable(AppScreen.Inventory.route) {
            InventoryScreen(
                db = db,
                onItemClick = { item ->
                    navController.navigateToInventoryItem(item)
                },
                onScreenShown = {
                    preferencesManager.markSuperRodIndicatorSeen()
                }
            )
        }

        composable(
            route = "inventory/detail/{itemId}",
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
            val selectedPokemonIdFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(SELECTED_POKEMON_RESULT_KEY, null)
            }
            val selectedPokemonId by selectedPokemonIdFlow.collectAsStateWithLifecycle(null)
            InventoryItemDetailScreen(
                db = db,
                preferencesManager = preferencesManager,
                itemId = itemId,
                selectedPokemonId = selectedPokemonId,
                onSelectionConsumed = {
                    backStackEntry.savedStateHandle.remove<String>(SELECTED_POKEMON_RESULT_KEY)
                },
                onSelectPokemonClick = { item ->
                    navController.navigate("inventory/detail/${item.ordinal}/select")
                },
                onItemUsed = {
                    navController.popBackStack(AppScreen.Home.route, inclusive = false)
                },
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(
            route = "inventory/detail/{itemId}/select",
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
            val item = Item.entries.getOrNull(itemId) ?: run {
                navController.navigateUp()
                return@composable
            }
            PokemonSelectionScreen(
                db = db,
                item = item,
                onPokemonSelected = { pokemon ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(SELECTED_POKEMON_RESULT_KEY, pokemon.id)
                    navController.popBackStack()
                },
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(AppScreen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}

private const val SELECTED_POKEMON_RESULT_KEY = "selectedPokemonId"
