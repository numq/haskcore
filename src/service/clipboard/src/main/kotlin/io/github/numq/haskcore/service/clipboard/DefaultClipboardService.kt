package io.github.numq.haskcore.service.clipboard

import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.Clipboard as SystemClipboard

internal class DefaultClipboardService(
    scope: CoroutineScope, private val systemClipboard: SystemClipboard
) : ClipboardService, ClipboardOwner {
    private val _clipboard = MutableStateFlow(Clipboard(text = currentText()))

    override val clipboard = _clipboard.asStateFlow()

    init {
        scope.launch {
            while (isActive) {
                val text = currentText()

                if (text != _clipboard.value.text) {
                    _clipboard.value = Clipboard(text = text)
                }

                delay(500L)
            }
        }
    }

    private fun currentText() = runCatching {
        systemClipboard.getContents(null)?.takeIf { contents ->
            contents.isDataFlavorSupported(DataFlavor.stringFlavor)
        }?.getTransferData(
            DataFlavor.stringFlavor
        ) as? String
    }.getOrNull()

    override suspend fun copyToClipboard(text: String) = Either.catch {
        systemClipboard.setContents(StringSelection(text), this)

        _clipboard.value = Clipboard(text = text)
    }

    override fun lostOwnership(clipboard: SystemClipboard, contents: Transferable) = Unit
}