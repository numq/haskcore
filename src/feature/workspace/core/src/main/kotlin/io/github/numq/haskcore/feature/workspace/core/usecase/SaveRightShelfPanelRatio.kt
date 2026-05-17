package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.workspace.core.WorkspaceService

class SaveRightShelfPanelRatio(
    private val workspaceService: WorkspaceService,
) : UseCase.Command<SaveRightShelfPanelRatio.Input> {
    data class Input(val ratio: Float)

    override suspend fun Raise<Throwable>.command(input: Input) =
        workspaceService.saveRightShelfPanelRatio(ratio = input.ratio).bind()
}