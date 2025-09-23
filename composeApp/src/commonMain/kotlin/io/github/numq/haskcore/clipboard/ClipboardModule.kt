package io.github.numq.haskcore.clipboard

import org.koin.dsl.bind
import org.koin.dsl.module

val clipboardModule = module {
    single { ClipboardService.Default(fileSystemService = get()) } bind ClipboardService::class
}