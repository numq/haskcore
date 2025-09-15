package io.github.numq.haskcore.filesystem.di

import io.github.numq.haskcore.filesystem.*
import io.github.numq.haskcore.filesystem.internal.FileSystemService
import org.koin.dsl.bind
import org.koin.dsl.module

val fileSystemModule = module {
    single { FileSystemService.Default() } bind FileSystemService.Default::class

    factory { CheckDirectory(get()) }

    factory { CheckExistence(get()) }

    factory { CheckFile(get()) }

    factory { Copy(get()) }

    factory { CreateDirectory(get()) }

    factory { CreateFile(get()) }

    factory { Delete(get()) }

    factory { ListDirectory(get()) }

    factory { Move(get()) }

    factory { Rename(get()) }
}