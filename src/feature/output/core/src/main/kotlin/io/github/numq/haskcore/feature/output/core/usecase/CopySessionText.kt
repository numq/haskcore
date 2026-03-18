package io.github.numq.haskcore.feature.output.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.output.core.OutputLine
import io.github.numq.haskcore.feature.output.core.OutputSession
import io.github.numq.haskcore.service.clipboard.ClipboardService

class CopySessionText(private val clipboardService: ClipboardService) : UseCase<CopySessionText.Input, Unit> {
    data class Input(val session: OutputSession)

    override suspend fun Raise<Throwable>.execute(input: Input) = clipboardService.copyToClipboard(
        text = input.session.lines.joinToString("\n", transform = OutputLine::text)
    ).bind()
}