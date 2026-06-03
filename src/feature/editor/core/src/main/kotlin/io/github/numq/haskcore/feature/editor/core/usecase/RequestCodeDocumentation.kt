package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.lsp.LspService

class RequestCodeDocumentation(
    private val path: String, private val lspService: LspService,
) : UseCase.Command<RequestCodeDocumentation.Input> {
    data class Input(val position: TextPosition)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        lspService.requestHover(path = path, position = position).bind()
    }
}