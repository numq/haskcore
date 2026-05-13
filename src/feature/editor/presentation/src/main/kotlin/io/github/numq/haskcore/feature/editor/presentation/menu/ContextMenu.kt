package io.github.numq.haskcore.feature.editor.presentation.menu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import io.github.numq.haskcore.common.presentation.overlay.menu.Menu
import io.github.numq.haskcore.common.presentation.overlay.menu.MenuItem
import io.github.numq.haskcore.common.presentation.overlay.menu.MenuState

@Composable
internal fun ContextMenu(
    menu: EditorMenu,
    runStack: () -> Unit,
    runCabal: () -> Unit,
    runGhc: () -> Unit,
    cut: () -> Unit,
    copy: () -> Unit,
    paste: () -> Unit,
    selectAll: () -> Unit,
    openMenu: (Offset) -> Unit,
    closeMenu: () -> Unit,
    content: @Composable () -> Unit,
) {
    Menu(
        state = when (menu) {
            is EditorMenu.Hidden -> MenuState.Hidden

            is EditorMenu.Visible -> MenuState.Visible(offset = Offset(x = menu.x, y = menu.y))
        }, onState = { state ->
            when (state) {
                is MenuState.Hidden -> closeMenu()

                is MenuState.Visible -> openMenu(Offset(x = state.offset.x, y = state.offset.y))
            }
        }, items = {
            listOf(
                MenuItem(label = "Run Stack", leadingIcon = {
                    ContextMenuIcon(imageVector = Icons.Rounded.PlayArrow)
                }, onClick = {
                    runStack()
                }), MenuItem(label = "Run Cabal", leadingIcon = {
                    ContextMenuIcon(imageVector = Icons.Rounded.PlayArrow)
                }, onClick = {
                    runCabal()
                }), MenuItem(label = "Run GHC", leadingIcon = {
                    ContextMenuIcon(imageVector = Icons.Rounded.PlayArrow)
                }, onClick = {
                    runGhc()
                }), MenuItem(label = "Cut", leadingIcon = {
                    ContextMenuIcon(imageVector = Icons.Rounded.ContentCut)
                }, onClick = {
                    cut()
                }), MenuItem(label = "Copy", leadingIcon = {
                    ContextMenuIcon(imageVector = Icons.Rounded.ContentCopy)
                }, onClick = {
                    copy()
                }), MenuItem(label = "Paste", leadingIcon = {
                    ContextMenuIcon(imageVector = Icons.Rounded.ContentPaste)
                }, onClick = {
                    paste()
                }), MenuItem(label = "Select All", leadingIcon = {
                    ContextMenuIcon(imageVector = Icons.Rounded.SelectAll)
                }, onClick = {
                    selectAll()
                })
            )
        }, content = content
    )
}