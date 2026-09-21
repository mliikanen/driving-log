package com.mikonoma.drivinglog.ui.theme

import androidx.compose.ui.graphics.Color

// The Petroleum palette of docs/color-palette.md, under the document's names. The theme is built from these and from the derived
// colors below; see the design of the `petroleum-color-theme` change for why each derived color exists.

val PetroleumDeep = Color(0xFF0F2027)
val OilSlickBlue = Color(0xFF203A43)
val RefineryTeal = Color(0xFF2C5364)
val FuelGaugeGold = Color(0xFFFFB703)
val RoadTripEmerald = Color(0xFF06D6A0)
val AsphaltDark = Color(0xFF12181B)
val CoolPlatinum = Color(0xFFF4F7F6)
val ExhaustFog = Color(0xFFE0E6ED)

// Derived. Where a raw palette color cannot be used for a role because its contrast is too low, the theme uses a tint or a shade of it.

/** Light tint of Refinery Teal: the primary of the dark scheme (Oil Slick Blue itself is 1.5:1 on Asphalt Dark). */
val TealTint = Color(0xFF8FC3D7)

/** Lighter tint: the dark scheme's secondary. */
val TealTintLight = Color(0xFFA3C6D4)

/** Pale blue-green containers of the light scheme, and their dark content. */
val PetroleumMist = Color(0xFFCFE3EC)
val PetroleumMistSecondary = Color(0xFFD5E7EF)

/** Light text on the dark scheme's containers. */
val PetroleumMistOn = Color(0xFFD2E5EE)
val PetroleumMistSecondaryOn = Color(0xFFD8E8F0)

/** Gold containers and their content. */
val GoldContainerLight = Color(0xFFFFE3A0)
val GoldOnContainerLight = Color(0xFF3B2A00)
val GoldContainerDark = Color(0xFF5A4200)

/** A deeper Emerald for distance text on the light scheme (5.7:1 on Cool Platinum; the bright emerald is 1.75:1). */
val EmeraldDeep = Color(0xFF006F53)

/** Text and content colors. */
val AsphaltText = Color(0xFF12181B)
val PlatinumText = Color(0xFFE1E8EA)
val LightVariantText = Color(0xFF3F4D55)
val DarkVariantText = Color(0xFFB7C4CA)

/** Variant surfaces, outlines and dividers. */
val DarkSurfaceVariant = Color(0xFF26343A)
val LightOutline = Color(0xFF66767F)
val DarkOutline = Color(0xFF7C8E97)
val DarkOutlineVariant = Color(0xFF2C3A41)

/** Errors are not in the palette (it has no red): Material's error colors, so an error never looks like the blue-greens. */
val ErrorLight = Color(0xFFB3261E)
val ErrorContainerLight = Color(0xFFF9DEDC)
val ErrorOnContainerLight = Color(0xFF410E0B)
val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
