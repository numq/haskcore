package io.github.numq.haskcore.stack

import io.github.numq.haskcore.stack.usecase.CreateStackProject
import org.koin.dsl.bind
import org.koin.dsl.module

internal val stackModule = module {
    single { StackService.Default() } bind StackService::class

    single { CreateStackProject(stackService = get()) }
}