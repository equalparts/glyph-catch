package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScreenHeader
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.ItemGlyphCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.metadata
import dev.equalparts.glyph_catch.ndotFontFamily

private data class InventoryUiItem(val item: Item, val quantity: Int)

@Composable
fun InventoryScreen(db: PokemonDatabase, onItemClick: (Item) -> Unit = {}, onScreenShown: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        onScreenShown()
    }

    val inventoryItems by db.inventoryDao().watchAllItems().collectAsStateWithLifecycle(emptyList())
    val ownedItems = remember(inventoryItems) {
        inventoryItems.mapNotNull { entry ->
            val item = Item.entries.getOrNull(entry.itemId) ?: return@mapNotNull null
            if (entry.quantity <= 0) return@mapNotNull null
            InventoryUiItem(item = item, quantity = entry.quantity)
        }.sortedBy { it.item.ordinal }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSizes.spacingXLarge)
    ) {
        AppScreenHeader(
            title = stringResource(R.string.inventory_title),
            subtitle = null
        )

        Spacer(modifier = Modifier.height(AppSizes.spacingLarge))

        if (ownedItems.isEmpty()) {
            InventoryEmptyState()
        } else {
            InventoryGrid(items = ownedItems, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun InventoryGrid(items: List<InventoryUiItem>, onItemClick: (Item) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
    ) {
        items(items) { uiItem ->
            InventoryItemCard(uiItem = uiItem, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun InventoryItemCard(uiItem: InventoryUiItem, onItemClick: (Item) -> Unit) {
    val metadata = uiItem.item.metadata()
    val name = stringResource(metadata.nameRes)
    val quantityLabel = stringResource(R.string.inventory_item_quantity, uiItem.quantity)

    val cardModifier = Modifier
        .aspectRatio(0.85f)
        .clickable { onItemClick(uiItem.item) }

    AppCard(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSizes.spacingExtraSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ItemGlyphCircle(
                item = uiItem.item,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(AppSizes.spacingMicro))

            val fontSize = when {
                name.length >= 10 -> MaterialTheme.typography.labelMedium.fontSize
                else -> MaterialTheme.typography.labelLarge.fontSize
            }
            Text(
                text = name,
                fontFamily = ndotFontFamily,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = quantityLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InventoryEmptyState() {
    AppEmptyState(
        primaryText = stringResource(R.string.inventory_empty_title),
        secondaryText = stringResource(R.string.inventory_empty_subtitle)
    )
}
