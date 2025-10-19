package io.github.numq.haskcore.workspace.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import io.github.numq.haskcore.split.HorizontalSplitPane
import io.github.numq.haskcore.workspace.presentation.dialog.WorkspaceDialog
import io.github.numq.haskcore.workspace.presentation.editor.WorkspaceEditor
import io.github.numq.haskcore.workspace.presentation.explorer.WorkspaceExplorer
import io.github.numq.haskcore.workspace.presentation.explorer.dialog.WorkspaceExplorerDialogView
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun WindowScope.WorkspaceView(feature: WorkspaceFeature) {
    val coroutineScope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    var explorerExpanded by remember { mutableStateOf(true) }

    val (horizontalSplitPanePercentage, setHorizontalSplitPanePercentage) = remember { mutableStateOf(.3f) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface {
            Column(
                modifier = Modifier.fillMaxHeight().padding(8.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Top)
            ) {
                IconButton(onClick = {
                    explorerExpanded = !explorerExpanded
                }) {
                    Icon(Icons.Default.Folder, "Explorer")
                }
            }
        }
        HorizontalSplitPane(
            percentage = if (explorerExpanded) horizontalSplitPanePercentage else 0f,
            onPercentageChange = setHorizontalSplitPanePercentage,
            modifier = Modifier.weight(1f),
            first = { width ->
                if (explorerExpanded) {
                    WorkspaceExplorer(
                        workspacePath = state.workspace.path,
                        clipboard = state.clipboard,
                        explorerNodes = state.explorerNodes,
                        explorerSelection = state.explorerSelection,
                        contextMenuState = state.contextMenuState,
                        openDialog = { dialog ->
                            coroutineScope.launch {
                                feature.execute(WorkspaceCommand.Dialog.Open(dialog = dialog))
                            }
                        },
                        openFile = { file ->
                            coroutineScope.launch {
                                feature.execute(WorkspaceCommand.Editor.Open(path = file.path, isReadOnly = false))
                            }
                        },
                        handleExplorerContextMenuCommand = { command ->
                            coroutineScope.launch {
                                feature.execute(command)
                            }
                        },
                        handleExplorerSelectionCommand = { command ->
                            coroutineScope.launch {
                                feature.execute(command)
                            }
                        },
                        handleExplorerOperationCommand = { command ->
                            coroutineScope.launch {
                                feature.execute(command)
                            }
                        })
                }
            },
            second = { width ->
                Column(
                    modifier = Modifier.fillMaxHeight().composed {
                        when {
                            explorerExpanded -> width(width)

                            else -> weight(1f)
                        }
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Bottom)
                ) {
                    WorkspaceEditor(
                        editor = state.editor,
                        handleEditorCommand = { command ->
                            coroutineScope.launch {
                                feature.execute(command)
                            }
                        })
                }
            })
    }

    when (val dialog = state.dialog) {
        null -> Unit

        is WorkspaceDialog.Explorer -> WorkspaceExplorerDialogView(
            dialog = dialog,
            createHaskellFile = { destination, name ->
                coroutineScope.launch {
                    feature.execute(
                        WorkspaceCommand.Explorer.Operation.CreateHaskellFile(destination = destination, name = name)
                    )
                }
            },
            createFile = { destination, name ->
                coroutineScope.launch {
                    feature.execute(
                        WorkspaceCommand.Explorer.Operation.CreateFile(
                            destination = destination, name = name
                        )
                    )
                }
            },
            createDirectory = { destination, name ->
                coroutineScope.launch {
                    feature.execute(
                        WorkspaceCommand.Explorer.Operation.CreateDirectory(
                            destination = destination, name = name
                        )
                    )
                }
            },
            rename = { node, name ->
                coroutineScope.launch {
                    feature.execute(WorkspaceCommand.Explorer.Operation.Rename(node = node, name = name))
                }
            },
            delete = {
                coroutineScope.launch {
                    feature.execute(WorkspaceCommand.Explorer.Operation.Delete)
                }
            },
            close = {
                coroutineScope.launch {
                    feature.execute(WorkspaceCommand.Dialog.Close)
                }
            })
    }
}