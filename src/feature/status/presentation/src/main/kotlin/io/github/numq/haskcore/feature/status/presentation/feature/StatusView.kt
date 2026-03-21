package io.github.numq.haskcore.feature.status.presentation.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.status.presentation.breadcrumbs.Breadcrumbs
import io.github.numq.haskcore.feature.status.presentation.tool.StatusToolItem
import io.github.numq.haskcore.platform.overlay.dialog.file.FileDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun StatusView(projectScope: Scope, handleError: (Throwable) -> Unit, navigateToPath: suspend (path: String) -> Unit) {
    val feature = koinInject<StatusFeature>(scope = projectScope)

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is StatusEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    val fileDialog = koinInject<FileDialog>(scope = projectScope)

    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxSize(),
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
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
}