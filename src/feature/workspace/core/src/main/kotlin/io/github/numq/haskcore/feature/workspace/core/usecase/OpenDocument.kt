package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.project.ProjectService

class OpenDocument(private val projectService: ProjectService) : UseCase.Command<OpenDocument.Input> {
    data class Input(val path: String)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        projectService.openDocument(path = path).bind()
    }
}