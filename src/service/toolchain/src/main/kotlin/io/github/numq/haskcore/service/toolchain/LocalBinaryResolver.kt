package io.github.numq.haskcore.service.toolchain

import arrow.core.Either
import java.io.File

internal class LocalBinaryResolver : BinaryResolver {
    private val pathSeparator = File.pathSeparator

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")

    override suspend fun findBinary(name: String, vararg paths: String) = Either.catch {
        val fullName = when {
            isWindows() -> "$name.exe"

            else -> name
        }

        val foldersToSearch = when {
            paths.isNotEmpty() -> paths.asSequence()

            else -> System.getenv("PATH")?.split(pathSeparator)?.asSequence() ?: emptySequence()
        }

        foldersToSearch.filter(String::isNotBlank).map { path ->
            File(path, fullName)
        }.firstOrNull { file ->
            file.exists() && file.canExecute()
        }?.absolutePath
    }
}