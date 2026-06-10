package io.github.numq.haskcore.feature.status.presentation.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.common.core.text.TextEncoding
import io.github.numq.haskcore.common.core.text.TextLineEnding
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.presentation.overlay.dialog.file.FileDialog
import io.github.numq.haskcore.feature.status.presentation.breadcrumbs.Breadcrumbs
import io.github.numq.haskcore.feature.status.presentation.item.StatusTextItem
import io.github.numq.haskcore.feature.status.presentation.item.StatusToolItem
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun StatusView(
    projectScope: Scope,
    handleError: (Throwable) -> Unit,
    textPosition: TextPosition?,
    textLineEnding: TextLineEnding?,
    textEncoding: TextEncoding?,
    navigateToPath: suspend (path: String) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val fileDialog = koinInject<FileDialog>(scope = projectScope)

    val feature = koinInject<StatusFeature>(scope = projectScope)

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is StatusEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxHeight().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Breadcrumbs(
            modifier = Modifier.weight(1f), pathSegments = state.status.pathSegments, navigateToPath = { path ->
                scope.launch {
                    navigateToPath(path)
                }
            })
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            textPosition?.let { position ->
                StatusTextItem(text = "Ln ${position.line + 1}, Col ${position.column + 1}")
            }
            textEncoding?.let { encoding ->
                StatusTextItem(text = encoding.charset.name())
            }
            textLineEnding?.let { lineEnding ->
                StatusTextItem(text = lineEnding.name)
            }
            StatusToolItem(name = "GHC", tool = state.status.ghc, selectPath = {
                scope.launch {
                    fileDialog.pickDirectory(title = "Select GHC directory")?.let { path ->
                        feature.execute(StatusCommand.SelectGhcPath(path = path))
                    }
                }
            }, resetPath = {
                scope.launch {
                    feature.execute(StatusCommand.ResetGhcPath)
                }
            })
            StatusToolItem(name = "Cabal", tool = state.status.cabal, selectPath = {
                scope.launch {
                    fileDialog.pickDirectory(title = "Select Cabal directory")?.let { path ->
                        feature.execute(StatusCommand.SelectCabalPath(path = path))
                    }
                }
            }, resetPath = {
                scope.launch {
                    feature.execute(StatusCommand.ResetCabalPath)
                }
            })
            StatusToolItem(name = "Stack", tool = state.status.stack, selectPath = {
                scope.launch {
                    fileDialog.pickDirectory(title = "Select Stack directory")?.let { path ->
                        feature.execute(StatusCommand.SelectStackPath(path = path))
                    }
                }
            }, resetPath = {
                scope.launch {
                    feature.execute(StatusCommand.ResetStackPath)
                }
            })
            StatusToolItem(name = "HLS", tool = state.status.hls, selectPath = {
                scope.launch {
                    fileDialog.pickDirectory(title = "Select HLS directory")?.let { path ->
                        feature.execute(StatusCommand.SelectHlsPath(path = path))
                    }
                }
            }, resetPath = {
                scope.launch {
                    feature.execute(StatusCommand.ResetHlsPath)
                }
            })
        }
    }
}