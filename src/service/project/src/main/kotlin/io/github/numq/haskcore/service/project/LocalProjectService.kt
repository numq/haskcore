package io.github.numq.haskcore.service.project

import kotlinx.coroutines.flow.map

internal class LocalProjectService(
    private val path: String, private val projectDataSource: ProjectDataSource
) : ProjectService {
    override val project = projectDataSource.projectData.map { projectData ->
        projectData.toProject(path = path)
    }

    override suspend fun renameProject(name: String) = projectDataSource.update { projectData ->
        projectData.copy(name = name)
    }.map { }

    override suspend fun openDocument(path: String) = projectDataSource.update { projectData ->
        when {
            projectData.openedDocumentPaths.any { openedDocumentPath ->
                openedDocumentPath == path
            } -> projectData.copy(activeDocumentPath = path)

            else -> projectData.copy(
                openedDocumentPaths = projectData.openedDocumentPaths + path, activeDocumentPath = path
            )
        }
    }.map { }

    override suspend fun closeDocument(path: String) = projectDataSource.update { projectData ->
        val openedDocumentPaths = projectData.openedDocumentPaths.filterNot { openedDocumentPath ->
            openedDocumentPath == path
        }

        val activeDocumentPath = when (projectData.activeDocumentPath) {
            path -> openedDocumentPaths.lastOrNull()

            else -> projectData.activeDocumentPath
        }

        projectData.copy(openedDocumentPaths = openedDocumentPaths, activeDocumentPath = activeDocumentPath)
    }.map { }
}