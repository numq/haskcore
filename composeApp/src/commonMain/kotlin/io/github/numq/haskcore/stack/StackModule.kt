package io.github.numq.haskcore.stack

import org.koin.dsl.bind
import org.koin.dsl.module

val stackModule = module {
    single { StackService.Default(fileSystemService = get(), processService = get()) } bind StackService::class
}