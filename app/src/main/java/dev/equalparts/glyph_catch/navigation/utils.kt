package dev.equalparts.glyph_catch.navigation

import android.net.Uri
import androidx.navigation.NavController
import dev.equalparts.glyph_catch.data.Pokemon

fun NavController.navigateToPokemon(speciesId: Int) {
    val species = Pokemon[speciesId]
    navigate(AppScreen.Caught.createRoute(species?.name ?: ""))
}

fun NavController.navigateToCaughtDetail(pokemonId: String, fromHome: Boolean = false) {
    val encodedId = Uri.encode(pokemonId)
    val prefix = if (fromHome) "home" else "caught"
    navigate("$prefix/detail/$encodedId")
}
