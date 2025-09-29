package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScreenHeader
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PokemonDatabase

@Suppress("UNUSED_PARAMETER")
@Composable
fun InventoryScreen(db: PokemonDatabase) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSizes.spacingXLarge)
    ) {
        Spacer(modifier = Modifier.height(AppSizes.spacingLarge))

        AppScreenHeader(
            title = stringResource(R.string.inventory_title),
            subtitle = null
        )

        Spacer(modifier = Modifier.height(AppSizes.spacingXLarge))

        InventoryEmptyState()
    }
}

@Composable
private fun InventoryEmptyState() {
    AppEmptyState(
        primaryText = stringResource(R.string.inventory_empty_title),
        secondaryText = stringResource(R.string.inventory_empty_subtitle)
    )
}
