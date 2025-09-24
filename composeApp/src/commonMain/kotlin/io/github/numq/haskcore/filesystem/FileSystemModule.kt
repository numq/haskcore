package io.github.numq.haskcore.filesystem

import org.koin.dsl.bind
import org.koin.dsl.module

val fileSystemModule = module {
    single { FileSystemService.Default() } bind FileSystemService::class
}