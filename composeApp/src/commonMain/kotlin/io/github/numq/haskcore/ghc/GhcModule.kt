package io.github.numq.haskcore.ghc

import org.koin.dsl.bind
import org.koin.dsl.module

internal val ghcModule = module {
    single { GhcService.Default() } bind GhcService::class
}