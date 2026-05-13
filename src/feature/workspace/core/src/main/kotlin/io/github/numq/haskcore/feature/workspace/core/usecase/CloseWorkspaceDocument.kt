package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.service.project.ProjectService
import io.github.numq.haskcore.common.core.usecase.UseCase

class CloseWorkspaceDocument(private val projectService: ProjectService) : UseCase<CloseWorkspaceDocument.Input, Unit> {
    data class Input(val path: String)

    override suspend fun Raise<Throwable>.execute(input: Input) = projectService.closeDocument(path = input.path).bind()
}