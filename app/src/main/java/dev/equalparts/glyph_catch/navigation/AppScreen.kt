package dev.equalparts.glyph_catch.navigation

import android.net.Uri
import androidx.annotation.DrawableRes
import dev.equalparts.glyph_catch.R

sealed class AppScreen(val route: String, @field:DrawableRes val iconRes: Int) {
    object Home : AppScreen("home", R.drawable.icon_home)
    object Pokedex : AppScreen("pokedex", R.drawable.icon_grid_dots)
    object Caught : AppScreen("caught?search={search}&showEggsOnly={showEggsOnly}", R.drawable.icon_pokeball) {
        private const val baseRoute = "caught"

        fun createRoute(search: String = "", showEggsOnly: Boolean = false): String {
            val builder = StringBuilder(baseRoute)
            var first = true

            if (search.isNotBlank()) {
                builder.append(if (first) "?" else "&").append("search=").append(Uri.encode(search))
                first = false
            }
            if (showEggsOnly) {
                builder.append(if (first) "?" else "&").append("showEggsOnly=true")
                first = false
            }
            return builder.toString()
        }
    }
    object Inventory : AppScreen("inventory", R.drawable.icon_bag)
    object Settings : AppScreen("settings", R.drawable.icon_grid_dots)

    companion object {
        val bottomNavItems = listOf(Home, Pokedex, Caught, Inventory)
    }
}
