package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.cabal.CabalBuildSystemService
import io.github.numq.haskcore.buildsystem.custom.CustomBuildSystemService
import io.github.numq.haskcore.buildsystem.ghc.GhcBuildSystemService
import io.github.numq.haskcore.buildsystem.runhaskell.RunHaskellBuildSystemService
import io.github.numq.haskcore.buildsystem.stack.StackBuildSystemService
import io.github.numq.haskcore.buildsystem.usecase.ObserveBuildStatus
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

internal val buildSystemModule = module {
    single { CustomBuildSystemService() }

    single { CabalBuildSystemService() }

    single { GhcBuildSystemService() }

    single { RunHaskellBuildSystemService() }

    single { StackBuildSystemService() }

    single {
        BuildSystemRepository.Default(
            customBuildSystemService = get(),
            cabalBuildSystemService = get(),
            ghcBuildSystemService = get(),
            runHaskellBuildSystemService = get(),
            stackBuildSystemService = get(),
            cabalService = get(),
            ghcService = get(),
            stackService = get()
        )
    } bind BuildSystemRepository::class onClose { it?.close() }

    single { ObserveBuildStatus(buildSystemRepository = get()) }
}