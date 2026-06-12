package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.workspace.core.Workspace
import io.github.numq.haskcore.feature.workspace.core.WorkspaceDocument
import io.github.numq.haskcore.feature.workspace.core.WorkspaceService
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.lsp.LspService
import io.github.numq.haskcore.service.lsp.connection.LspConnection
import io.github.numq.haskcore.service.project.ProjectService
import io.github.numq.haskcore.service.toolchain.Toolchain
import io.github.numq.haskcore.service.toolchain.ToolchainService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ObserveWorkspace(
    private val workspaceService: WorkspaceService,
    private val documentService: DocumentService,
    private val lspService: LspService,
    private val projectService: ProjectService,
    private val toolchainService: ToolchainService,
) : UseCase.Query<Flow<Workspace>> {
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeLsp() = toolchainService.toolchain.flatMapLatest { toolchain ->
        flow<Unit> {
            val hlsPath = (toolchain as? Toolchain.Detected)?.hls?.getOrNull()?.path

            if (hlsPath != null) {
                lspService.start(hlsPath = hlsPath).getOrElse { throwable ->
                    throw throwable
                }

                try {
                    awaitCancellation()
                } finally {
                    lspService.stop().getOrElse(::println) // todo
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun Raise<Throwable>.query(): Flow<Workspace> {
        val syncedPathsForCurrentConnection = mutableSetOf<String>()

        return channelFlow {
            launch {
                observeLsp().collect()
            }

            launch {
                lspService.connection.filterIsInstance<LspConnection.Connected>().flatMapLatest { connection ->
                    syncedPathsForCurrentConnection.clear()

                    projectService.project.map { project -> project.openedDocumentPaths }
                }.collect { openedPaths ->
                    openedPaths.forEach { path ->
                        if (path !in syncedPathsForCurrentConnection) {
                            val text = documentService.readDocument(path = path).getOrNull()?.content

                            if (text != null) {
                                lspService.openDocument(path = path, text = text).getOrElse(::println) // todo

                                syncedPathsForCurrentConnection.add(path)
                            }
                        }
                    }
                }
            }

            combine(
                projectService.project, workspaceService.workspace
            ) { project, workspace ->
                var activeDocument: WorkspaceDocument? = null

                val documents = project.openedDocumentPaths.map { openedDocumentPath ->
                    val document = documentService.readMetadata(path = openedDocumentPath).map { metadata ->
                        with(metadata) {
                            WorkspaceDocument(path = path, name = name, language = language)
                        }
                    }.getOrElse { throwable ->
                        throw throwable
                    }

                    if (openedDocumentPath == project.activeDocumentPath) {
                        activeDocument = document
                    }

                    document
                }

                workspace.copy(
                    path = project.path, name = project.name, documents = documents, activeDocument = activeDocument
                )
            }.collect(::send)
        }
    }
}