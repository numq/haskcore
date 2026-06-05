package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion
import io.github.numq.haskcore.feature.editor.core.toCodeSuggestion
import io.github.numq.haskcore.service.lsp.LspService
import io.github.numq.haskcore.service.lsp.completion.LspCompletion

class GetCodeSuggestions(
    private val path: String, private val lspService: LspService,
) : UseCase.Exchange<GetCodeSuggestions.Input, List<CodeSuggestion>> {
    data class Input(val position: TextPosition)

    override suspend fun Raise<Throwable>.exchange(input: Input) = with(input) {
        lspService.getCompletions(path = path, position = position).bind().map(LspCompletion::toCodeSuggestion)
    }
}