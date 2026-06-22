package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.workspace.core.WorkspaceService

class ToggleFullscreen(private val workspaceService: WorkspaceService) : UseCase.Action {
    override suspend fun Raise<Throwable>.action() = workspaceService.toggleFullscreen().bind()
}