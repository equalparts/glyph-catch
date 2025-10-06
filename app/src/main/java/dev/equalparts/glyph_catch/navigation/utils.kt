package dev.equalparts.glyph_catch.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import dev.equalparts.glyph_catch.data.Pokemon

fun NavController.navigateToPokemon(speciesId: Int) {
    val species = Pokemon[speciesId]
    val searchRoute = AppScreen.Caught.createRoute(species?.name ?: "")
    navigate(searchRoute) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToCaughtDetail(pokemonId: String, fromHome: Boolean = false) {
    val encodedId = Uri.encode(pokemonId)
    val prefix = if (fromHome) "home" else "caught"
    navigate("$prefix/detail/$encodedId")
}
