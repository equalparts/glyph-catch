package dev.equalparts.glyph_catch.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon

fun NavController.navigateToPokemon(speciesId: Int) {
    val species = Pokemon[speciesId]
    val searchQuery = species?.name.orEmpty()
    val targetRoute = AppScreen.Caught.createRoute(searchQuery)
    val shouldRestoreState = searchQuery.isBlank()
    navigate(targetRoute) {
        popUpTo(graph.findStartDestination().id) {
            saveState = shouldRestoreState
        }
        launchSingleTop = true
        restoreState = shouldRestoreState
    }
}

fun NavController.navigateToCaughtDetail(pokemonId: String, fromHome: Boolean = false) {
    val encodedId = Uri.encode(pokemonId)
    val prefix = if (fromHome) "home" else "caught"
    navigate("$prefix/detail/$encodedId")
}

fun NavController.navigateToInventoryItem(item: Item) {
    navigate("inventory/detail/${item.ordinal}")
}
