package com.mikonoma.drivinglog.landing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.ui.LandingIcons
import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors

private val Gap = 16.dp
private val Margin = 16.dp
private val MinTileHeight = 96.dp
private val MaxTileHeight = 200.dp
private val MaxTileWidth = 260.dp

/** The test tag of a tile. */
private fun LandingTileId.tag(): String = when (this) {
    LandingTileId.VEHICLES -> "landing_vehicles"
    LandingTileId.LOG_EVENT -> "landing_log_event"
    LandingTileId.TRIP -> "landing_trip"
    LandingTileId.PLACEHOLDER -> "landing_more"
}

@Composable
fun LandingScreen(
    processor: LandingProcessor,
    onShowVehicles: () -> Unit,
    onShowAddVehicle: () -> Unit,
) {
    val state by processor.states.collectAsState()

    LaunchedEffect(processor) {
        processor.sideEffects.collect { effect ->
            when (effect) {
                LandingEffect.ShowVehicles -> onShowVehicles()
                LandingEffect.ShowAddVehicle -> onShowAddVehicle()
            }
        }
    }

    LandingContent(
        state = state,
        // The first tile is "Vehicles" or, with none, "Add vehicle": one tile, whose action follows what it says.
        onFirstTile = { processor.dispatch(if (state.hasVehicles) LandingIntent.OpenVehicles else LandingIntent.AddVehicle) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingContent(state: LandingState, onFirstTile: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                CenterAlignedTopAppBar(title = { Text("Driving Log") }, colors = drivingLogTopAppBarColors())
                HeaderDivider()
            }
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val available = maxHeight
            // Two tiles and the gaps fit in the width, two in the height (the height matters in landscape), within limits: a tile is square in portrait and wider than tall
            // in landscape, where the width is plentiful and a large font scale needs it for the labels. When even the smallest height does not fit the grid scrolls.
            val tileWidth = ((maxWidth - Margin * 2 - Gap) / 2).coerceAtMost(MaxTileWidth)
            val tileHeight = minOf(tileWidth, (maxHeight - Margin * 2 - Gap) / 2).coerceIn(MinTileHeight, MaxTileHeight)
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = available).padding(Margin),
                verticalArrangement = Arrangement.spacedBy(Gap, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val tiles = state.tiles
                Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                    LandingTileCard(tiles[0], tileWidth, tileHeight, onClick = onFirstTile)
                    LandingTileCard(tiles[1], tileWidth, tileHeight, onClick = {})
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                    LandingTileCard(tiles[2], tileWidth, tileHeight, onClick = {})
                    LandingTileCard(tiles[3], tileWidth, tileHeight, onClick = {})
                }
                Spacer(Modifier.height(ScreenBottomSpace))
            }
        }
    }
}

/**
 * One action: a Material 3 [Card] used as it is meant to be. An available tile takes the theme's action colors; a tile that is not enabled is the same card with
 * `enabled = false`, so Material's own disabled colors, semantics and input handling apply and none of them is written here. The icon takes the card's content color.
 */
@Composable
private fun LandingTileCard(tile: LandingTile, width: Dp, height: Dp, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = tile.enabled,
        modifier = Modifier.size(width, height).testTag(tile.id.tag()),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The tile is an icon and shows no text; the icon's own description is the tile's name (the clickable card merges it), empty only while the first tile loads.
            Icon(LandingIcons.of(tile.icon), contentDescription = tile.name.ifEmpty { null }, modifier = Modifier.size(64.dp))
        }
    }
}
