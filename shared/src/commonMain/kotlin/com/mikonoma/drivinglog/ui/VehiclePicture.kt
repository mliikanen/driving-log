package com.mikonoma.drivinglog.ui

import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * A vehicle's picture, loaded from [uri] (a `file://` URI of the locally stored picture today, and any URI Coil can load later, such as an
 * `https://` one). While it loads, when it cannot be loaded and when [uri] is null there is no request: the icon of the vehicle type on a tile
 * is drawn instead (the generic car when [type] is null, which is only the edit form before the saved vehicle has loaded), labelled "Vehicle type: <name>". The caller sets the size, which is also the size the picture is decoded at.
 */
@Composable
fun VehiclePicture(
    uri: String?,
    type: VehicleType?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    placeholderDescription: String? = type?.let { "Vehicle type: ${it.label}" },
) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (uri == null) {
            PlaceholderIcon(type, placeholderDescription)
        } else {
            SubcomposeAsyncImage(
                model = uri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { PlaceholderIcon(type, placeholderDescription) },
                error = { PlaceholderIcon(type, placeholderDescription) },
            )
        }
    }
}

@Composable
private fun PlaceholderIcon(type: VehicleType?, description: String?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = if (type != null) VehicleIcons.of(type) else VehicleIcons.Car,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(1f),
        )
    }
}
