package io.github.numq.haskcore.clipboard

import org.koin.dsl.bind
import org.koin.dsl.module

internal val clipboardModule = module {
    single { ClipboardRepository.Default() } bind ClipboardRepository::class

    single { GetClipboard(clipboardRepository = get()) }
}