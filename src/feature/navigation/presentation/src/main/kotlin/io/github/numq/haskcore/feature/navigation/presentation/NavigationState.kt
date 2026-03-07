package io.github.numq.haskcore.feature.navigation.presentation

import io.github.numq.haskcore.feature.navigation.core.Destination

internal data class NavigationState(val destinations: List<Destination>)