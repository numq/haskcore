package io.github.numq.haskcore.feature.workspace.presentation.feature.view

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.presentation.container.Container
import io.github.numq.haskcore.common.presentation.tab.CloseableTabs
import io.github.numq.haskcore.feature.workspace.core.Workspace
import io.github.numq.haskcore.feature.workspace.core.WorkspaceDocument
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceCommand
import io.github.numq.haskcore.feature.workspace.presentation.shelf.ShelfPanelContentHandle
import io.github.numq.haskcore.feature.workspace.presentation.shelf.ShelfToolContent
import kotlinx.coroutines.launch

@Composable
internal fun WorkspaceContent(
    workspace: Workspace,
    execute: suspend (WorkspaceCommand) -> Unit,
    leftWeight: Float,
    localLeftRatio: Float,
    changeLocalLeftRatio: (Float) -> Unit,
    rightWeight: Float,
    localRightRatio: Float,
    changeLocalRightRatio: (Float) -> Unit,
    centerWeight: Float,
    explorer: @Composable () -> Unit,
    log: @Composable () -> Unit,
    editor: @Composable (path: String?, language: Language?) -> Unit,
) {
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val totalWidth = constraints.maxWidth.toFloat()

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leftWeight > 0f) {
                Box(modifier = Modifier.weight(leftWeight)) {
                    workspace.shelf.leftPanel.activeTool?.let { tool ->
                        ShelfToolContent(tool = tool, explorer = explorer, log = log)
                    }
                }

                ShelfPanelContentHandle(
                    totalWidth = totalWidth, onPositionChange = { deltaX ->
                        if (totalWidth > 0f) {
                            val currentRatio = localLeftRatio

                            val delta = deltaX / totalWidth

                            val localLeftRatio = (localLeftRatio + delta).coerceIn(.05f, .4f)

                            if (localLeftRatio != currentRatio) {
                                changeLocalLeftRatio(localLeftRatio)
                            }
                        }
                    })
            }

            Box(modifier = Modifier.weight(centerWeight), contentAlignment = Alignment.Center) {
                workspace.activeDocument?.let { activeDocument ->
                    Container {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            CloseableTabs(
                                modifier = Modifier.fillMaxWidth(),
                                items = workspace.documents,
                                activeItem = workspace.activeDocument,
                                getItemName = WorkspaceDocument::name,
                                select = { document ->
                                    scope.launch {
                                        execute(WorkspaceCommand.OpenDocument(document = document))
                                    }
                                },
                                close = { document ->
                                    scope.launch {
                                        execute(WorkspaceCommand.CloseDocument(document = document))
                                    }
                                })
                            Box(
                                modifier = Modifier.weight(1f), contentAlignment = Alignment.Center
                            ) {
                                editor(activeDocument.path, activeDocument.language)
                            }
                        }
                    }
                }
            }

            if (rightWeight > 0f) {
                ShelfPanelContentHandle(
                    totalWidth = totalWidth, onPositionChange = { deltaX ->
                        if (totalWidth > 0f) {
                            val currentRatio = localRightRatio

                            val delta = -deltaX / totalWidth

                            val localRightRatio = (localRightRatio + delta).coerceIn(.05f, .4f)

                            if (localRightRatio != currentRatio) {
                                changeLocalRightRatio(localRightRatio)
                            }
                        }
                    })

                Box(modifier = Modifier.weight(rightWeight)) {
                    workspace.shelf.rightPanel.activeTool?.let { tool ->
                        ShelfToolContent(tool = tool, explorer = explorer, log = log)
                    }
                }
            }
        }
    }
}