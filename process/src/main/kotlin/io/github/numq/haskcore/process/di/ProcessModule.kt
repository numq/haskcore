package io.github.numq.haskcore.process.di

import io.github.numq.haskcore.process.internal.ProcessService
import org.koin.dsl.bind
import org.koin.dsl.module

val processModule = module {
    single { ProcessService.Default() } bind ProcessService::class
}