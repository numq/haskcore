package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.cabal.CabalBuildSystemService
import io.github.numq.haskcore.buildsystem.ghc.GhcBuildSystemService
import io.github.numq.haskcore.buildsystem.runhaskell.RunHaskellBuildSystemService
import io.github.numq.haskcore.buildsystem.stack.StackBuildSystemService
import io.github.numq.haskcore.buildsystem.usecase.ObserveBuildStatus
import io.github.numq.haskcore.buildsystem.usecase.Synchronize
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

internal val buildSystemModule = module {
    single { BuildSystemService() }

    single { CabalBuildSystemService.Default() } bind CabalBuildSystemService::class

    single { GhcBuildSystemService.Default() } bind GhcBuildSystemService::class

    single { RunHaskellBuildSystemService.Default() } bind RunHaskellBuildSystemService::class

    single { StackBuildSystemService.Default() } bind StackBuildSystemService::class

    single { (path: String) ->
        BuildSystemRepository.Default(
            path = path,
            customBuildSystemService = get(),
            cabalBuildSystemService = get(),
            ghcBuildSystemService = get(),
            runHaskellBuildSystemService = get(),
            stackBuildSystemService = get()
        )
    } bind BuildSystemRepository::class onClose { it?.close() }

    single { (path: String) ->
        ObserveBuildStatus(buildSystemRepository = get { parametersOf(path) })
    }

    single { (path: String) ->
        Synchronize(buildSystemRepository = get { parametersOf(path) })
    }
}