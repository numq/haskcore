package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.service.project.ProjectService

class OpenWorkspaceDocument(private val projectService: ProjectService) : UseCase<OpenWorkspaceDocument.Input, Unit> {
    data class Input(val path: String)

    override suspend fun Raise<Throwable>.execute(input: Input) = projectService.openDocument(path = input.path).bind()
}