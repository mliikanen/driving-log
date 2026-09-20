package com.mikonoma.drivinglog.home

import dev.zacsweers.metro.Inject
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor

@Inject
class HomeProcessor : PresentationProcessor<HomeIntent, HomeViewState, HomeSideEffect>(HomeViewState()) {

    // The Home screen has no intents yet.
    override suspend fun map(intent: HomeIntent): Action<HomeViewState, HomeSideEffect>? = null
}
