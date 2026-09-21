package com.mikonoma.drivinglog.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/** Every screen's app bar is a dark header, in both schemes (the palette's "dominant app headers"): Petroleum Deep with light content. */
val HeaderContainer = PetroleumDeep
val HeaderContent = CoolPlatinum

/** The colors of a screen's app bar (`TopAppBar` and `CenterAlignedTopAppBar` both take them). */
@Composable
fun drivingLogTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = HeaderContainer,
    scrolledContainerColor = HeaderContainer,
    navigationIconContentColor = HeaderContent,
    titleContentColor = HeaderContent,
    actionIconContentColor = HeaderContent,
)

/** A text button placed in the header ("Save"): its default is the primary color, which is unreadable on the dark header. */
@Composable
fun headerTextButtonColors(): ButtonColors = ButtonDefaults.textButtonColors(
    contentColor = HeaderContent,
    disabledContentColor = HeaderContent.copy(alpha = 0.38f),
)

/** Put under the app bar: in the dark scheme the header (1.07:1) is hardly told apart from the background, so a thin line separates them. */
@Composable
fun HeaderDivider() {
    if (DrivingLogTheme.isDark) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
