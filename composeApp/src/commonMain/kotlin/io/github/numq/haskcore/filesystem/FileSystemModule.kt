package io.github.numq.haskcore.filesystem

import org.koin.dsl.bind
import org.koin.dsl.module

val fileSystemModule = module {
    single { VirtualFileSystem.Default() } bind VirtualFileSystem::class

    single { FileSystemService.Default(virtualFileSystem = get()) } bind FileSystemService::class
}