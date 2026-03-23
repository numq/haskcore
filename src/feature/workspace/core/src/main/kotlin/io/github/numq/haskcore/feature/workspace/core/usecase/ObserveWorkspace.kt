package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.workspace.core.Workspace
import io.github.numq.haskcore.feature.workspace.core.WorkspaceDocument
import io.github.numq.haskcore.feature.workspace.core.WorkspaceService
import io.github.numq.haskcore.service.project.ProjectService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveWorkspace(
    private val workspaceService: WorkspaceService, private val projectService: ProjectService
) : UseCase<Unit, Flow<Workspace>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = combine(
        flow = projectService.project, flow2 = workspaceService.workspace, transform = { project, workspace ->
            val documents = project.openedDocumentPaths.map { openedDocumentPath ->
                val name = workspaceService.getName(path = openedDocumentPath).getOrElse { throwable ->
                    throw throwable // todo
                }

                WorkspaceDocument(path = openedDocumentPath, name = name)
            }

            workspace.copy(
                path = project.path,
                name = project.name,
                documents = documents,
                activeDocumentPath = project.activeDocumentPath
            )
        })
}