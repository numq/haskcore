package io.github.numq.haskcore.feature.explorer.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.service.project.ProjectService
import io.github.numq.haskcore.common.core.usecase.UseCase

class OpenFile(private val projectService: ProjectService) : UseCase<OpenFile.Input, Unit> {
    data class Input(val path: String)

    override suspend fun Raise<Throwable>.execute(input: Input) = projectService.openDocument(path = input.path).bind()
}