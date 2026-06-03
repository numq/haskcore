package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.lsp.LspService

class DismissCodeDocumentation(
    private val path: String, private val lspService: LspService,
) : UseCase.Command<Unit> {
    override suspend fun Raise<Throwable>.command(input: Unit) = with(input) {
        lspService.dismissHover(path = path).bind()
    }
}