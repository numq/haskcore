package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.workspace.core.WorkspaceService

class SaveDimensions(private val workspaceService: WorkspaceService) : UseCase.Command<SaveDimensions.Input> {
    data class Input(val x: Float, val y: Float, val width: Float, val height: Float)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        workspaceService.saveDimensions(x = x, y = y, width = width, height = height).bind()
    }
}