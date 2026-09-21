package com.mikonoma.drivinglog.ui

import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import com.mikonoma.drivinglog.ui.color.VehicleTones
import com.mikonoma.drivinglog.ui.color.toColor
import com.mikonoma.drivinglog.ui.theme.DrivingLogTheme
import com.mikonoma.drivinglog.vehicle.domain.Rgb
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
 * is drawn instead, tinted from the vehicle's [color] (the generic car when [type] is null, which is only the edit form before the saved vehicle has loaded), labelled "Vehicle type: <name>". The caller sets the size, which is also the size the picture is decoded at.
 */
@Composable
fun VehiclePicture(
    uri: String?,
    type: VehicleType?,
    /** The vehicle's color, already animated by the screen (see `rememberAnimatedColor`): the icon and its tile are drawn from it. */
    color: Rgb,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    placeholderDescription: String? = type?.let { "Vehicle type: ${it.label}" },
) {
    val shape = RoundedCornerShape(12.dp)
    val tones = VehicleTones.of(color, DrivingLogTheme.isDark)
    Box(modifier.clip(shape).background(tones.container.toColor()), contentAlignment = Alignment.Center) {
        if (uri == null) {
            PlaceholderIcon(type, tones.icon.toColor(), placeholderDescription)
        } else {
            SubcomposeAsyncImage(
                model = uri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { PlaceholderIcon(type, tones.icon.toColor(), placeholderDescription) },
                error = { PlaceholderIcon(type, tones.icon.toColor(), placeholderDescription) },
            )
        }
    }
}

@Composable
private fun PlaceholderIcon(type: VehicleType?, tint: Color, description: String?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = if (type != null) VehicleIcons.of(type) else VehicleIcons.Car,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(1f),
        )
    }
}
