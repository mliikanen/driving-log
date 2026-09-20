package com.mikonoma.drivinglog.home

import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

data class HomeViewState(
    val isEmpty: Boolean = true,
) : ViewState

sealed interface HomeIntent : ViewIntent

sealed interface HomeSideEffect : SideEffect
