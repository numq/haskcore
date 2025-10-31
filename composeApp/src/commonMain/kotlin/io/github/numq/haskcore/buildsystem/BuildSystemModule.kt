package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.presentation.console.BuildSystemConsoleFeature
import io.github.numq.haskcore.buildsystem.presentation.console.BuildSystemConsoleReducer
import io.github.numq.haskcore.buildsystem.presentation.console.BuildSystemConsoleState
import io.github.numq.haskcore.buildsystem.presentation.toolbar.BuildSystemToolbarFeature
import io.github.numq.haskcore.buildsystem.presentation.toolbar.BuildSystemToolbarReducer
import io.github.numq.haskcore.buildsystem.presentation.toolbar.BuildSystemToolbarState
import io.github.numq.haskcore.buildsystem.usecase.*
import io.github.numq.haskcore.feature.factory.CommandStrategy
import io.github.numq.haskcore.feature.factory.FeatureFactory
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

internal val buildSystemModule = module {
    single { (path: String) ->
        BuildSystemRepository.Default(path = path, discoveryService = get(), stackService = get())
    } bind BuildSystemRepository::class onClose { it?.close() }

    single { (path: String) ->
        ClearBuildOutput(buildSystemRepository = get { parametersOf(path) })
    }

    single { (path: String) ->
        ObserveBuildOutput(buildSystemRepository = get { parametersOf(path) })
    }

    single { (path: String) ->
        ObserveBuildSystemStatus(buildSystemRepository = get { parametersOf(path) })
    }

    single { (path: String) ->
        OperateBuildSystem(buildSystemRepository = get { parametersOf(path) })
    }

    single { (path: String) ->
        SynchronizeBuildSystem(buildSystemRepository = get { parametersOf(path) })
    }

    single { (path: String) ->
        BuildSystemConsoleFeature(
            feature = FeatureFactory().create(
                initialState = BuildSystemConsoleState(),
                reducer = BuildSystemConsoleReducer(
                    clearBuildOutput = get { parametersOf(path) },
                    observeBuildOutput = get { parametersOf(path) },
                    observeBuildSystemStatus = get { parametersOf(path) }),
                strategy = CommandStrategy.Immediate
            )
        )
    }

    single { (path: String) ->
        BuildSystemToolbarFeature(
            feature = FeatureFactory().create(
                initialState = BuildSystemToolbarState(), reducer = BuildSystemToolbarReducer(
                    observeBuildSystemStatus = get { parametersOf(path) },
                    operateBuildSystem = get { parametersOf(path) },
                    synchronizeBuildSystem = get { parametersOf(path) },
                ), strategy = CommandStrategy.Immediate
            )
        )
    }
}