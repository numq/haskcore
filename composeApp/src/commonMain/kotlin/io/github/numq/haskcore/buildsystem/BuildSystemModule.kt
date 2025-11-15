package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.cabal.CabalBuildSystemService
import io.github.numq.haskcore.buildsystem.ghc.GhcBuildSystemService
import io.github.numq.haskcore.buildsystem.runhaskell.RunHaskellBuildSystemService
import io.github.numq.haskcore.buildsystem.stack.StackBuildSystemService
import io.github.numq.haskcore.buildsystem.usecase.ObserveBuildStatus
import io.github.numq.haskcore.buildsystem.usecase.SynchronizeBuildSystem
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

internal val buildSystemModule = module {
    single { BuildSystemService() }

    single { CabalBuildSystemService.Default() } bind CabalBuildSystemService::class

    single { GhcBuildSystemService.Default() } bind GhcBuildSystemService::class

    single { RunHaskellBuildSystemService.Default() } bind RunHaskellBuildSystemService::class

    single { StackBuildSystemService.Default() } bind StackBuildSystemService::class

    single {
        BuildSystemRepository.Default(
            customBuildSystemService = get(),
            cabalBuildSystemService = get(),
            ghcBuildSystemService = get(),
            runHaskellBuildSystemService = get(),
            stackBuildSystemService = get()
        )
    } bind BuildSystemRepository::class onClose { it?.close() }

    single {
        ObserveBuildStatus(buildSystemRepository = get())
    }

    single {
        SynchronizeBuildSystem(buildSystemRepository = get())
    }
}