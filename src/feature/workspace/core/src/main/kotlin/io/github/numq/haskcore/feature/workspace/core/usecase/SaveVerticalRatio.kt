package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.workspace.core.WorkspaceService

class SaveVerticalRatio(private val workspaceService: WorkspaceService) : UseCase.Command<SaveVerticalRatio.Input> {
    data class Input(val ratio: Float)

    override suspend fun Raise<Throwable>.command(input: Input) =
        workspaceService.saveVerticalRatio(ratio = input.ratio).bind()
}