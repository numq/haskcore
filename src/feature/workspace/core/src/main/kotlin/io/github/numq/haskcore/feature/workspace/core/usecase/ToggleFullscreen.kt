package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.workspace.core.WorkspaceService

class ToggleFullscreen(private val workspaceService: WorkspaceService) : UseCase.Command<Unit> {
    override suspend fun Raise<Throwable>.command(input: Unit) = with(input) {
        workspaceService.toggleFullscreen().bind()
    }
}