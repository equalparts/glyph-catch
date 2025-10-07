package dev.equalparts.glyph_catch.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.ItemGlyphCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.ActiveItem
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.ItemMetadata
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.metadata
import dev.equalparts.glyph_catch.ndotFontFamily
import dev.equalparts.glyph_catch.util.rememberActiveItemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InventoryItemDetailScreen(db: PokemonDatabase, itemId: Int, onBackClick: () -> Unit) {
    val inventoryFlow = remember(db) { db.inventoryDao().watchAllItems() }
    val inventoryItems by inventoryFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val item = remember(itemId) { Item.entries.getOrNull(itemId) }
    val inventoryEntry = remember(inventoryItems) { inventoryItems.firstOrNull { it.itemId == itemId } }

    AppScaffoldWithTopBar(
        title = stringResource(R.string.inventory_item_title),
        onBackClick = onBackClick
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppSizes.spacingLarge, vertical = AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingLarge)
        ) {
            if (item == null || inventoryEntry == null || inventoryEntry.quantity <= 0) {
                AppEmptyState(
                    primaryText = stringResource(R.string.inventory_item_missing_title),
                    secondaryText = stringResource(R.string.inventory_item_missing_subtitle)
                )
            } else {
                InventoryItemDetailContent(db = db, item = item, quantity = inventoryEntry.quantity)
            }
        }
    }
}

@Composable
private fun InventoryItemDetailContent(db: PokemonDatabase, item: Item, quantity: Int) {
    val metadata = item.metadata()
    val name = stringResource(metadata.nameRes)
    val status by rememberActiveItemStatus(db, item)
    val coroutineScope = rememberCoroutineScope()
    var isActivating by remember { mutableStateOf(false) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ItemGlyphCircle(
                item = item,
                contentDescription = name,
                modifier = Modifier.size(120.dp)
            )

            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.inventory_item_quantity, quantity),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = ndotFontFamily
            )

            Text(
                text = stringResource(metadata.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (metadata.durationMinutes != null) {
        Button(
            onClick = {
                if (!status.isActive) {
                    coroutineScope.launch {
                        isActivating = true
                        try {
                            activateItem(db, item, metadata)
                        } finally {
                            isActivating = false
                        }
                    }
                }
            },
            enabled = !status.isActive && !isActivating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (status.isActive) {
                    stringResource(R.string.inventory_item_using_button, name)
                } else {
                    stringResource(R.string.inventory_item_use_button, name)
                }
            )
        }

        if (status.isActive) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            db.activeItemDao().deactivateItem(item.ordinal)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.inventory_item_cancel_button))
            }
        }
    } else {
        Text(
            text = stringResource(R.string.inventory_item_use_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private suspend fun activateItem(db: PokemonDatabase, item: Item, metadata: ItemMetadata) {
    val durationMinutes = metadata.durationMinutes ?: return
    val now = System.currentTimeMillis()
    val expiresAt = now + durationMinutes * 60_000L
    withContext(Dispatchers.IO) {
        db.activeItemDao().activateItem(
            ActiveItem(
                itemId = item.ordinal,
                activatedAt = now,
                expiresAt = expiresAt
            )
        )
    }
}
