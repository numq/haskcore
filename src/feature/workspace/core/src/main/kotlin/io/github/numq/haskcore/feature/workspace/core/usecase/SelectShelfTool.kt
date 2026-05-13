package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.workspace.core.ShelfTool
import io.github.numq.haskcore.feature.workspace.core.WorkspaceService

class SelectShelfTool(private val workspaceService: WorkspaceService) : UseCase<SelectShelfTool.Input, Unit> {
    data class Input(val tool: ShelfTool)

    override suspend fun Raise<Throwable>.execute(input: Input) =
        workspaceService.selectShelfTool(tool = input.tool).bind()
}