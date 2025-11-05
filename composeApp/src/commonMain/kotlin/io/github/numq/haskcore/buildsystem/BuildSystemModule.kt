package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.usecase.ObserveBuildStatus
import io.github.numq.haskcore.buildsystem.usecase.Synchronize
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

internal val buildSystemModule = module {
    single { (path: String) ->
        BuildSystemRepository.Default(path = path, discoveryService = get(), stackService = get())
    } bind BuildSystemRepository::class onClose { it?.close() }

    single { (path: String) ->
        ObserveBuildStatus(buildSystemRepository = get { parametersOf(path) })
    }

    single { (path: String) ->
        Synchronize(buildSystemRepository = get { parametersOf(path) })
    }
}