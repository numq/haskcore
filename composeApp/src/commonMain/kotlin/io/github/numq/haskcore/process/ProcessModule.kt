package io.github.numq.haskcore.process

import org.koin.dsl.bind
import org.koin.dsl.module

val processModule = module {
    single { ProcessService.Default() } bind ProcessService::class
}