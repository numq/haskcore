package io.github.numq.haskcore.stack

import io.github.numq.haskcore.stack.usecase.*
import org.koin.dsl.bind
import org.koin.dsl.module

internal val stackModule = module {
    single { StackService.Default() } bind StackService::class

    single { StackRepository.Default(stackService = get()) } bind StackRepository::class

    single { ObserveStackProject(stackRepository = get()) }

    single { CreateStackProject(outputRepository = get(), stackRepository = get()) }

    single { BuildStackProject(outputRepository = get(), stackRepository = get()) }

    single { RunStackProject(outputRepository = get(), stackRepository = get()) }

    single { TestStackProject(outputRepository = get(), stackRepository = get()) }

    single { CleanStackProject(outputRepository = get(), stackRepository = get()) }
}