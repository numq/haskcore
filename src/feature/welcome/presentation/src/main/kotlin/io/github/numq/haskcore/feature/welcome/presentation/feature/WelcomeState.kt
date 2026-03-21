package io.github.numq.haskcore.feature.welcome.presentation.feature

import io.github.numq.haskcore.feature.welcome.core.RecentProject

internal data class WelcomeState(val title: String, val recentProjects: List<RecentProject> = emptyList())