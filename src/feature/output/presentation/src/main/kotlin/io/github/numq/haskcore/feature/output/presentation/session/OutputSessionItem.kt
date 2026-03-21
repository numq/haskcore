package io.github.numq.haskcore.feature.output.presentation.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.output.core.OutputLine
import io.github.numq.haskcore.feature.output.core.OutputSession
import io.github.numq.haskcore.feature.output.presentation.line.OutputLineItem
import io.github.numq.haskcore.feature.output.presentation.menu.OutputMenu
import io.github.numq.haskcore.platform.overlay.menu.Menu
import io.github.numq.haskcore.platform.overlay.menu.MenuItem
import io.github.numq.haskcore.platform.overlay.menu.MenuState

@Composable
internal fun OutputSessionItem(
    session: OutputSession, menu: OutputMenu, openMenu: (Offset) -> Unit, copyText: () -> Unit, closeMenu: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(session.lines.size) {
        if (session.lines.isNotEmpty()) {
            listState.animateScrollToItem(session.lines.size - 1)
        }
    }

    Menu(
        state = when (menu) {
            is OutputMenu.Hidden -> MenuState.Hidden

            is OutputMenu.Visible -> MenuState.Visible(offset = Offset(x = menu.x, y = menu.y))
        }, onState = { state ->
            when (state) {
                is MenuState.Hidden -> closeMenu()

                is MenuState.Visible -> openMenu(Offset(x = state.offset.x, y = state.offset.y))
            }
        }, items = {
            listOf(
                MenuItem(
                    label = "Copy text", leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }, enabled = session.lines.isNotEmpty(), onClick = copyText
                )
            )
        }, content = {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(items = session.lines, key = OutputLine::id, contentType = { it::class }) { line ->
                    OutputLineItem(line = line)
                }
            }
        })
}