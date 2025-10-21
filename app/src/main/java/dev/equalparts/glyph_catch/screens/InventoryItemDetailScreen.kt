package dev.equalparts.glyph_catch.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.equalparts.glyph_catch.AppEmptyState
import dev.equalparts.glyph_catch.AppScaffoldWithTopBar
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.nameRes
import dev.equalparts.glyph_catch.screens.inventory.InventoryItemContent
import dev.equalparts.glyph_catch.util.ItemUsageError
import dev.equalparts.glyph_catch.util.ItemUsageResult
import dev.equalparts.glyph_catch.util.useItemOnPokemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InventoryItemDetailScreen(
    db: PokemonDatabase,
    preferencesManager: PreferencesManager,
    itemId: Int,
    selectedPokemonId: String? = null,
    onSelectionConsumed: () -> Unit = {},
    onSelectPokemonClick: (Item) -> Unit = {},
    onItemUsed: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val inventoryFlow = remember(db) { db.inventoryDao().watchAllItems() }
    val inventoryItems by inventoryFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val item = remember(itemId) { Item.entries.getOrNull(itemId) }
    val inventoryEntry = remember(inventoryItems) { inventoryItems.firstOrNull { it.itemId == itemId } }

    val pokemonDao = remember(db) { db.pokemonDao() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingPokemon by remember { mutableStateOf<CaughtPokemon?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }
    var isProcessingSelection by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPokemonId, item) {
        if (selectedPokemonId != null) {
            if (item == null) {
                Toast.makeText(context, context.getString(R.string.item_usage_error_unknown), Toast.LENGTH_SHORT).show()
                onSelectionConsumed()
            } else {
                val fetched = withContext(Dispatchers.IO) { pokemonDao.getCaughtPokemon(selectedPokemonId) }
                if (fetched != null) {
                    pendingPokemon = fetched
                    showConfirmation = true
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.item_usage_error_invalid_pokemon),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                onSelectionConsumed()
            }
        }
    }

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
                InventoryItemContent(
                    db = db,
                    item = item,
                    quantity = inventoryEntry.quantity,
                    preferencesManager = preferencesManager,
                    onSelectPokemonClick = onSelectPokemonClick
                )
            }
        }
    }

    if (showConfirmation && pendingPokemon != null && item != null) {
        val selectedPokemon = pendingPokemon!!
        val itemName = stringResource(item.nameRes())
        val speciesName = Pokemon[selectedPokemon.speciesId]?.name ?: stringResource(R.string.common_unknown)
        val displayName = selectedPokemon.nickname ?: speciesName

        AlertDialog(
            onDismissRequest = {
                if (!isProcessingSelection) {
                    showConfirmation = false
                    pendingPokemon = null
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isProcessingSelection,
                    onClick = {
                        if (isProcessingSelection) return@TextButton
                        scope.launch {
                            isProcessingSelection = true
                            try {
                                val result = useItemOnPokemon(db, preferencesManager, item, selectedPokemon.id)
                                when (result) {
                                    is ItemUsageResult.Success -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.item_usage_success_generic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onItemUsed()
                                    }
                                    is ItemUsageResult.Error -> {
                                        val message = when (result.reason) {
                                            ItemUsageError.ITEM_NOT_AVAILABLE -> context.getString(
                                                R.string.item_usage_error_no_item,
                                                itemName
                                            )
                                            ItemUsageError.INVALID_POKEMON -> context.getString(
                                                R.string.item_usage_error_invalid_pokemon
                                            )
                                            else -> context.getString(R.string.item_usage_error_unknown)
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.item_usage_error_unknown),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isProcessingSelection = false
                                showConfirmation = false
                                pendingPokemon = null
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.item_usage_confirm_button))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isProcessingSelection,
                    onClick = {
                        if (!isProcessingSelection) {
                            showConfirmation = false
                            pendingPokemon = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.item_usage_cancel_button))
                }
            },
            title = {
                Text(stringResource(R.string.item_usage_confirm_title, itemName))
            },
            text = {
                Text(stringResource(R.string.item_usage_confirm_message, itemName, displayName))
            }
        )
    }
}
