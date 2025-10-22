package dev.equalparts.glyph_catch.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.ItemGlyphCircle
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.ActiveItem
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.descriptionRes
import dev.equalparts.glyph_catch.data.effectDurationMinutes
import dev.equalparts.glyph_catch.data.nameRes
import dev.equalparts.glyph_catch.ndotFontFamily
import dev.equalparts.glyph_catch.util.rememberActiveItemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InventoryItemContent(
    db: PokemonDatabase,
    item: Item,
    quantity: Int,
    preferencesManager: PreferencesManager,
    onSelectPokemonClick: (Item) -> Unit
) {
    val name = stringResource(item.nameRes())
    val description = stringResource(item.descriptionRes())
    val durationMinutes = item.effectDurationMinutes()

    InventoryItemInfoCard(
        item = item,
        name = name,
        description = description,
        quantity = quantity
    )

    when {
        durationMinutes != null -> TimedItemActions(db, item, name, durationMinutes)
        item == Item.REPEL -> RepelToggleCard(preferencesManager)
        else -> UseOnPokemonButton(name, quantity) { onSelectPokemonClick(item) }
    }
}

@Composable
private fun InventoryItemInfoCard(item: Item, name: String, description: String, quantity: Int) {
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
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TimedItemActions(db: PokemonDatabase, item: Item, itemName: String, durationMinutes: Int) {
    val status by rememberActiveItemStatus(db, item)
    val scope = rememberCoroutineScope()
    var isActivating by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!status.isActive) {
                scope.launch {
                    isActivating = true
                    try {
                        activateItem(db, item, durationMinutes)
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
                stringResource(R.string.inventory_item_using_button, itemName)
            } else {
                stringResource(R.string.inventory_item_use_button, itemName)
            }
        )
    }

    if (status.isActive) {
        OutlinedButton(
            onClick = {
                scope.launch {
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
}

@Composable
private fun RepelToggleCard(preferencesManager: PreferencesManager) {
    val flow = remember(preferencesManager) { preferencesManager.watchRepelActive() }
    val repelActive by flow.collectAsStateWithLifecycle(preferencesManager.isRepelActive)

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSizes.spacingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.item_repel_toggle_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(AppSizes.spacingTiny))
                    Text(
                        text = stringResource(R.string.item_repel_toggle_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = repelActive,
                    onCheckedChange = { isEnabled ->
                        preferencesManager.isRepelActive = isEnabled
                    }
                )
            }
        }
    }
}

@Composable
private fun UseOnPokemonButton(itemName: String, quantity: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = quantity > 0,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.inventory_item_use_on_pokemon_button, itemName))
    }
}

private suspend fun activateItem(db: PokemonDatabase, item: Item, durationMinutes: Int) {
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
