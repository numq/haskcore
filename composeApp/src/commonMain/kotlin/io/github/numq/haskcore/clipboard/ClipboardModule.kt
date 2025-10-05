package io.github.numq.haskcore.clipboard

import org.koin.dsl.bind
import org.koin.dsl.module

internal val clipboardModule = module {
    single { ClipboardService.Default(fileSystemService = get()) } bind ClipboardService::class

    single { GetClipboard(clipboardService = get()) }
}