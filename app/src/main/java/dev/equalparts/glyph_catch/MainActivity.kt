package dev.equalparts.glyph_catch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.navigation.AppNavigationGraph
import dev.equalparts.glyph_catch.navigation.AppScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = PokemonDatabase.getInstance(this)

        setContent {
            AppTheme {
                MainScreen(db)
            }
        }
    }
}

@Composable
fun MainScreen(db: PokemonDatabase) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MainBottomBar(
                currentDestination = currentDestination,
                onNavigate = { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        MainContent(
            db = db,
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
private fun MainContent(db: PokemonDatabase, navController: NavHostController, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        AppNavigationGraph(
            navController = navController,
            db = db
        )
    }
}

@Composable
private fun MainBottomBar(currentDestination: NavDestination?, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = CatchColors.Black,
        contentColor = CatchColors.White,
        tonalElevation = AppSizes.none
    ) {
        AppScreen.bottomNavItems.forEach { screen ->
            val targetRoute = screen.bottomNavRoute()
            val selected = currentDestination.isRouteSelected(screen)

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.iconRes),
                        contentDescription = screen.route
                    )
                },
                label = {
                    Text(
                        text = screen.bottomNavLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = selected,
                onClick = { onNavigate(targetRoute) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CatchColors.Red,
                    selectedTextColor = CatchColors.Red,
                    unselectedIconColor = CatchColors.White.copy(alpha = 0.6f),
                    unselectedTextColor = CatchColors.White.copy(alpha = 0.6f),
                    indicatorColor = CatchColors.Black
                )
            )
        }
    }
}

private fun NavDestination?.isRouteSelected(screen: AppScreen): Boolean {
    if (this == null) return false
    return hierarchy.any { destination ->
        val route = destination.route
        when (screen) {
            AppScreen.Home ->
                route == AppScreen.Home.route ||
                    route?.startsWith("home/detail") == true ||
                    route == AppScreen.Settings.route
            AppScreen.Caught -> route?.startsWith("caught") == true
            else -> route == screen.route
        }
    }
}

private fun AppScreen.bottomNavRoute(): String = when (this) {
    AppScreen.Caught -> AppScreen.Caught.createRoute()
    else -> route.substringBefore("?")
}

@Composable
private fun AppScreen.bottomNavLabel(): String = when (this) {
    AppScreen.Home -> stringResource(R.string.nav_home)
    AppScreen.Pokedex -> stringResource(R.string.nav_pokedex)
    AppScreen.Caught -> stringResource(R.string.nav_caught)
    AppScreen.Inventory -> stringResource(R.string.nav_bag)
    AppScreen.Settings -> stringResource(R.string.nav_settings)
}
