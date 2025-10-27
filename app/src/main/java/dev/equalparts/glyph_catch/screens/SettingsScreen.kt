package dev.equalparts.glyph_catch.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.ndotFontFamily
import dev.equalparts.glyph_catch.screens.settings.DebugSettingsScreen
import dev.equalparts.glyph_catch.screens.settings.SettingsDestination
import dev.equalparts.glyph_catch.screens.settings.SettingsSectionList
import dev.equalparts.glyph_catch.screens.settings.SleepSettingsScreen
import dev.equalparts.glyph_catch.screens.settings.GraphicsSettingsScreen
import dev.equalparts.glyph_catch.screens.settings.WeatherSettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    var destination by remember { mutableStateOf(SettingsDestination.SectionList) }

    BackHandler(enabled = destination != SettingsDestination.SectionList) {
        destination = SettingsDestination.SectionList
    }

    SettingsScaffold(
        destination = destination,
        onDestinationChange = { destination = it },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    destination: SettingsDestination,
    onDestinationChange: (SettingsDestination) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            SettingsTopBar(
                destination = destination,
                onDestinationChange = onDestinationChange,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        SettingsContent(
            destination = destination,
            onDestinationChange = onDestinationChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSizes.spacingLarge)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    destination: SettingsDestination,
    onDestinationChange: (SettingsDestination) -> Unit,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(destination.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = ndotFontFamily
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (destination == SettingsDestination.SectionList) {
                        onBackClick()
                    } else {
                        onDestinationChange(SettingsDestination.SectionList)
                    }
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back)
                )
            }
        },
        windowInsets = WindowInsets(
            left = AppSizes.none,
            top = AppSizes.none,
            right = AppSizes.none,
            bottom = AppSizes.none
        )
    )
}

@Composable
private fun SettingsContent(
    destination: SettingsDestination,
    onDestinationChange: (SettingsDestination) -> Unit,
    modifier: Modifier
) {
    when (destination) {
        SettingsDestination.SectionList -> SettingsSectionList(
            modifier = modifier,
            onSectionSelected = onDestinationChange
        )

        SettingsDestination.Weather -> WeatherSettingsScreen(modifier = modifier)
        SettingsDestination.Sleep -> SleepSettingsScreen(modifier = modifier)
        SettingsDestination.Graphics -> GraphicsSettingsScreen(modifier = modifier)
        SettingsDestination.Debug -> DebugSettingsScreen(modifier = modifier)
    }
}
