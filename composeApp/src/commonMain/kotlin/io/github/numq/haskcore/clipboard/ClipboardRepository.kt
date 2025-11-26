package io.github.numq.haskcore.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal interface ClipboardRepository {
    val clipboard: StateFlow<Clipboard>

    suspend fun cut(paths: List<String>): Result<Unit>

    suspend fun copy(paths: List<String>): Result<Unit>

    suspend fun paste(path: String): Result<Unit>

    suspend fun clearClipboard(): Result<Unit>

    class Default() : ClipboardRepository {
        private val _clipboard = MutableStateFlow<Clipboard>(Clipboard.Empty)

        override val clipboard = _clipboard.asStateFlow()

        override suspend fun cut(paths: List<String>) = runCatching {
            _clipboard.value = Clipboard.Cut(paths = paths)
        }

        override suspend fun copy(paths: List<String>) = runCatching {
            _clipboard.value = Clipboard.Copy(paths = paths)
        }

        override suspend fun paste(path: String) = runCatching {
            _clipboard.update { clipboard ->
                when (clipboard) {
                    is Clipboard.Empty, is Clipboard.Copy -> clipboard

                    is Clipboard.Cut -> Clipboard.Empty
                }
            }
        }

        override suspend fun clearClipboard() = runCatching {
            _clipboard.value = Clipboard.Empty
        }
    }
}