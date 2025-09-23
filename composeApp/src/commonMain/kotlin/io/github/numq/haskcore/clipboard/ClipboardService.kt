package io.github.numq.haskcore.clipboard

import io.github.numq.haskcore.filesystem.FileSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ClipboardService {
    val clipboard: StateFlow<Clipboard>

    suspend fun cut(paths: List<String>): Result<Unit>

    suspend fun copy(paths: List<String>): Result<Unit>

    suspend fun paste(path: String): Result<Unit>

    suspend fun removeFromClipboard(path: String): Result<Unit>

    suspend fun clearClipboard(): Result<Unit>

    class Default(private val fileSystemService: FileSystemService) : ClipboardService {
        private val _clipboard = MutableStateFlow<Clipboard>(Clipboard.Empty)

        override val clipboard = _clipboard.asStateFlow()

        override suspend fun cut(paths: List<String>) = runCatching {
            _clipboard.value = Clipboard.Cut(paths = paths)
        }

        override suspend fun copy(paths: List<String>) = runCatching {
            _clipboard.value = Clipboard.Copy(paths = paths)
        }

        override suspend fun paste(path: String) = runCatching {
            when (val clipboard = clipboard.value) {
                is Clipboard.Empty -> return@runCatching

                is Clipboard.Cut -> {
                    clipboard.paths.forEach { fromPath ->
                        val toPath = "$path/${fromPath.substringAfterLast("/")}"

                        fileSystemService.move(fromPath = fromPath, toPath = toPath, overwrite = false).getOrThrow()
                    }

                    _clipboard.value = Clipboard.Empty
                }

                is Clipboard.Copy -> clipboard.paths.forEach { fromPath ->
                    val toPath = "$path/${fromPath.substringAfterLast("/")}"

                    fileSystemService.copy(fromPath = fromPath, toPath = toPath, overwrite = false).getOrThrow()
                }
            }
        }

        override suspend fun removeFromClipboard(path: String) = runCatching {
            _clipboard.value = when (val clipboard = clipboard.value) {
                is Clipboard.Empty -> clipboard

                is Clipboard.Cut -> when {
                    clipboard.paths.size > 1 -> clipboard.copy(paths = clipboard.paths - path)

                    else -> Clipboard.Empty
                }

                is Clipboard.Copy -> when {
                    clipboard.paths.size > 1 -> clipboard.copy(paths = clipboard.paths - path)

                    else -> Clipboard.Empty
                }
            }
        }

        override suspend fun clearClipboard() = runCatching {
            _clipboard.value = Clipboard.Empty
        }
    }
}