package io.github.numq.haskcore.stack

import io.github.numq.haskcore.stack.usecase.*
import org.koin.dsl.bind
import org.koin.dsl.module

internal val stackModule = module {
    single { StackService.Default() } bind StackService::class

    single { StackRepository.Default(stackService = get()) } bind StackRepository::class

    single { GetStackProject(stackRepository = get()) }

    single { CreateStackProject(stackRepository = get()) }

    single { BuildStackProject(stackRepository = get()) }

    single { RunStackProject(stackRepository = get()) }

    single { TestStackProject(stackRepository = get()) }

    single { CleanStackProject(stackRepository = get()) }
}