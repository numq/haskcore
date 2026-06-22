package io.github.numq.haskcore.feature.workspace.presentation.feature.view

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.presentation.container.Container
import io.github.numq.haskcore.common.presentation.tab.CloseableTabs
import io.github.numq.haskcore.feature.workspace.core.WorkspaceDocument
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceCommand
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceState
import io.github.numq.haskcore.feature.workspace.presentation.feature.window.WorkspaceWindow
import io.github.numq.haskcore.feature.workspace.presentation.shelf.ShelfPanelContentHandle
import io.github.numq.haskcore.feature.workspace.presentation.shelf.ShelfToolContent
import kotlinx.coroutines.launch

@Composable
internal fun WorkspaceViewReady(
    state: WorkspaceState.Ready,
    execute: suspend (WorkspaceCommand) -> Unit,
    icon: Painter,
    explorer: @Composable () -> Unit,
    log: @Composable () -> Unit,
    editor: @Composable (path: String?, language: Language?) -> Unit,
    execution: @Composable () -> Unit,
    status: @Composable () -> Unit,
    output: (@Composable () -> Unit)?,
) {
    val scope = rememberCoroutineScope()

    WorkspaceWindow(
        workspace = state.workspace,
        icon = icon,
        toggleFullscreen = {
            scope.launch {
                execute(WorkspaceCommand.ToggleFullscreen)
            }
        },
        selectShelfTool = { tool ->
            scope.launch {
                execute(WorkspaceCommand.SelectShelfTool(tool = tool))
            }
        },
        saveLeftShelfPanelRatio = { ratio ->
            scope.launch {
                execute(WorkspaceCommand.SaveLeftShelfPanelRatio(ratio = ratio))
            }
        },
        saveRightShelfPanelRatio = { ratio ->
            scope.launch {
                execute(WorkspaceCommand.SaveRightShelfPanelRatio(ratio = ratio))
            }
        },
        saveVerticalRatio = { ratio ->
            scope.launch {
                execute(WorkspaceCommand.SaveVerticalRatio(ratio = ratio))
            }
        },
        saveWindowDimensions = { x, y, width, height ->
            scope.launch {
                execute(
                    WorkspaceCommand.SaveDimensions(
                        x = x, y = y, width = width, height = height
                    )
                )
            }
        },
        close = { x, y, width, height ->
            scope.launch {
                execute(
                    WorkspaceCommand.Close(
                        windowX = x, windowY = y, windowWidth = width, windowHeight = height
                    )
                )
            }
        },
        execution = execution,
        status = status,
        output = output,
        content = { leftWeight, leftRatio, changeLeftRatio, centerWeight, rightWeight, rightRatio, changeRightRatio ->
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                val totalWidth = constraints.maxWidth.toFloat()

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leftWeight > 0f) {
                        Box(modifier = Modifier.weight(leftWeight)) {
                            state.workspace.shelf.leftPanel.activeTool?.let { tool ->
                                ShelfToolContent(tool = tool, explorer = explorer, log = log)
                            }
                        }

                        ShelfPanelContentHandle(
                            totalWidth = totalWidth,
                            currentRatio = leftRatio,
                            isInverted = false,
                            onRatioChange = { newRatio ->
                                val validRatio = newRatio.coerceIn(.05f, .4f)

                                if (leftRatio != validRatio) {
                                    changeLeftRatio(validRatio)
                                }
                            })
                    }

                    Box(
                        modifier = Modifier.weight(centerWeight), contentAlignment = Alignment.Center
                    ) {
                        Container {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Top
                            ) {
                                state.workspace.activeDocument?.let { activeDocument ->
                                    CloseableTabs(
                                        modifier = Modifier.fillMaxWidth(),
                                        items = state.workspace.documents,
                                        activeItem = activeDocument,
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
                            totalWidth = totalWidth,
                            currentRatio = rightRatio,
                            isInverted = true,
                            onRatioChange = { newRatio ->
                                val validRatio = newRatio.coerceIn(.05f, .4f)

                                if (rightRatio != validRatio) {
                                    changeRightRatio(validRatio)
                                }
                            })

                        Box(modifier = Modifier.weight(rightWeight)) {
                            state.workspace.shelf.rightPanel.activeTool?.let { tool ->
                                ShelfToolContent(tool = tool, explorer = explorer, log = log)
                            }
                        }
                    }
                }
            }
        })
}