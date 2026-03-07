package io.github.numq.haskcore.feature.navigation.core

import arrow.core.Either

interface NavigationService {
    suspend fun getInitialWorkspace(path: String): Either<Throwable, InitialWorkspace?>
}