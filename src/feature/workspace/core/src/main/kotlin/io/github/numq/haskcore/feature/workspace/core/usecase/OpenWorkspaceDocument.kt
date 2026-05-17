package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.project.ProjectService

class OpenWorkspaceDocument(private val projectService: ProjectService) : UseCase.Command<OpenWorkspaceDocument.Input> {
    data class Input(val path: String)

    override suspend fun Raise<Throwable>.command(input: Input) = projectService.openDocument(path = input.path).bind()
}