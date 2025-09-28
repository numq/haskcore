package io.github.numq.haskcore.stack

import org.koin.dsl.bind
import org.koin.dsl.module

internal val stackModule = module {
    single { StackService.Default(fileSystemService = get(), processService = get()) } bind StackService::class
}