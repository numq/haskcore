package io.github.numq.haskcore.feature.explorer.presentation.feature

import io.github.numq.haskcore.feature.explorer.core.ExplorerNode
import io.github.numq.haskcore.feature.explorer.core.ExplorerPosition
import io.github.numq.haskcore.feature.explorer.core.ExplorerTree
import io.github.numq.haskcore.feature.explorer.presentation.dialog.ExplorerDialog
import kotlinx.coroutines.flow.Flow

internal sealed interface ExplorerCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, TOGGLE_EXPLORER_NODE, SAVE_EXPLORER_POSITION, OPEN_PATH, CREATE_DIRECTORY, CREATE_FILE, MOVE, CUT, COPY, PASTE, DELETE
    }

    data class HandleFailure(val throwable: Throwable) : ExplorerCommand

    data class ShowDialog(val dialog: ExplorerDialog) : ExplorerCommand

    data object Initialize : ExplorerCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<ExplorerTree>) : ExplorerCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateExplorerTree(val explorerTree: ExplorerTree) : ExplorerCommand

    data class ToggleExplorerNode(val node: ExplorerNode.Directory) : ExplorerCommand {
        val key = Key.TOGGLE_EXPLORER_NODE
    }

    data class SelectExplorerNode(val node: ExplorerNode?) : ExplorerCommand

    data class SaveExplorerPosition(val position: ExplorerPosition) : ExplorerCommand {
        val key = Key.SAVE_EXPLORER_POSITION
    }

    data object SaveExplorerPositionSuccess : ExplorerCommand

    data class OpenPath(val path: String) : ExplorerCommand {
        val key = Key.OPEN_PATH
    }

    sealed interface Menu : ExplorerCommand {
        data class Open(val node: ExplorerNode, val x: Float, val y: Float) : Menu

        data object Close : Menu

        sealed interface Action : Menu {
            sealed interface CreateFile : Menu {
                data class ShowDialog(val node: ExplorerNode) : CreateFile

                data class Confirmation(val node: ExplorerNode, val name: String) : CreateFile {
                    val key = Key.CREATE_FILE
                }

                data object Success : CreateFile

                data class Failure(val throwable: Throwable) : CreateFile
            }

            sealed interface CreateDirectory : Menu {
                data class ShowDialog(val node: ExplorerNode) : CreateDirectory

                data class Confirmation(
                    val node: ExplorerNode, val name: String,
                ) : CreateDirectory {
                    val key = Key.CREATE_DIRECTORY
                }

                data object Success : CreateDirectory

                data class Failure(val throwable: Throwable) : CreateDirectory
            }

            data class Move(val node: ExplorerNode) : Action {
                val key = Key.MOVE
            }

            data class Cut(val node: ExplorerNode) : Action {
                val key = Key.CUT
            }

            data class Copy(val node: ExplorerNode) : Action {
                val key = Key.COPY
            }

            data class Paste(val node: ExplorerNode) : Action {
                val key = Key.PASTE
            }

            data class Delete(val node: ExplorerNode) : Action {
                val key = Key.DELETE
            }
        }
    }
}