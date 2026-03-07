package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.workspace.core.WorkspaceService

class SaveDimensions(private val workspaceService: WorkspaceService) : UseCase<SaveDimensions.Input, Unit> {
    data class Input(val x: Float, val y: Float, val width: Float, val height: Float, val isFullscreen: Boolean)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input) {
        workspaceService.saveDimensions(
            x = x, y = y, width = width, height = height, isFullscreen = isFullscreen
        ).bind()
    }
}