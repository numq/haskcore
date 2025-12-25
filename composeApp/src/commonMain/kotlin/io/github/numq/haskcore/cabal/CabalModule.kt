package io.github.numq.haskcore.cabal

import org.koin.dsl.bind
import org.koin.dsl.module

internal val cabalModule = module {
    single { CabalService.Default() } bind CabalService::class
}