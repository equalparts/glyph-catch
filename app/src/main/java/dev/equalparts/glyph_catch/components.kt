package dev.equalparts.glyph_catch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import dev.equalparts.glyph_catch.util.PokemonSpriteUtils

@Composable
fun AppScreenHeader(modifier: Modifier = Modifier, title: String, subtitle: String? = null) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.Companion.height(AppSizes.spacingMedium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = ndotFontFamily,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Normal
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.Companion.height(AppSizes.spacingMedium))
    }
}

@Composable
fun AppEmptyState(modifier: Modifier = Modifier, primaryText: String, secondaryText: String? = null) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSizes.spacingSmall)
        ) {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            secondaryText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier,
            onClick = onClick,
            shape = RoundedCornerShape(AppSizes.cardCornerRadius),
            colors = colors
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(AppSizes.cardCornerRadius),
            colors = colors
        ) {
            content()
        }
    }
}

@Composable
fun AppSectionHeader(modifier: Modifier = Modifier, text: String) {
    Spacer(modifier = Modifier.height(AppSizes.spacingXLarge))
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(AppSizes.spacingMedium))
}

@Composable
fun PokedexSpriteCircle(
    modifier: Modifier = Modifier,
    pokemonId: Int,
    pokemonName: String,
    caught: Boolean,
    size: Dp? = AppSizes.pokemonImageSize
) {
    val background = if (caught) CatchColors.Black else CatchColors.MediumGray
    PokemonSpriteCircle(
        modifier = modifier,
        pokemonId = pokemonId,
        pokemonName = pokemonName,
        backgroundColor = background,
        showSprite = caught,
        size = size
    )
}

@Composable
fun PokemonSpriteCircle(
    modifier: Modifier = Modifier,
    pokemonId: Int,
    pokemonName: String,
    backgroundColor: Color = CatchColors.Black,
    showSprite: Boolean = true,
    contentPadding: Dp = AppSizes.spacingTiny,
    size: Dp? = AppSizes.pokemonImageSize
) {
    Box(
        modifier = modifier
            .then(if (size != null) Modifier.size(size) else Modifier)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val resourceId = remember(pokemonId, context) {
            PokemonSpriteUtils.getMatrixResourceId(context, pokemonId)
        }

        if (showSprite && resourceId != 0) {
            Image(
                painter = painterResource(resourceId),
                contentDescription = pokemonName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun PokemonTypeChips(types: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppSizes.spacingTiny)
    ) {
        types.forEach { type ->
            PokemonTypeChip(type = type)
        }
    }
}

@Composable
fun PokemonTypeChip(modifier: Modifier = Modifier, type: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppSizes.chipCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = AppSizes.spacingSmall, vertical = AppSizes.spacingMicro)
    ) {
        Text(
            text = type.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PokemonLevelChip(level: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppSizes.chipCornerRadius))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = AppSizes.spacingSmall, vertical = AppSizes.spacingMicro)
    ) {
        Text(
            text = stringResource(R.string.components_level_chip, level),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffoldWithTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (paddingValues: androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = ndotFontFamily
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        },
        content = content
    )
}
