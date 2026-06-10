package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.analysis.CodeDocumentation
import io.github.numq.haskcore.feature.editor.core.toDocumentation
import io.github.numq.haskcore.service.lsp.LspService

class GetCodeDocumentation(
    private val path: String, private val lspService: LspService,
) : UseCase.Exchange<GetCodeDocumentation.Input, CodeDocumentation?> {
    data class Input(val language: Language, val position: TextPosition)

    override suspend fun Raise<Throwable>.exchange(input: Input) = with(input) {
        when (language) {
            is Language.Haskell -> lspService.getHover(path = path, position = position).bind()?.toDocumentation()

            is Language.Undefined -> null
        }
    }
}