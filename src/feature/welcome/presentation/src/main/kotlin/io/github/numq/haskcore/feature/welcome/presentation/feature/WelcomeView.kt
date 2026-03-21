package io.github.numq.haskcore.feature.welcome.presentation.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import io.github.numq.haskcore.feature.welcome.core.RecentProject
import io.github.numq.haskcore.feature.welcome.presentation.button.ActionButton
import io.github.numq.haskcore.feature.welcome.presentation.logo.DistortionLogo
import io.github.numq.haskcore.feature.welcome.presentation.recent.RecentProjectItem
import io.github.numq.haskcore.platform.font.LogoFont
import io.github.numq.haskcore.platform.font.MonoFont
import io.github.numq.haskcore.platform.overlay.dialog.file.FileDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope

@Composable
fun WelcomeView(
    applicationScope: Scope,
    handleError: (Throwable) -> Unit,
    title: String,
    icon: Painter,
    logoFont: LogoFont,
    monoFont: MonoFont,
    openProject: (path: String, name: String?) -> Unit,
    exitApplication: () -> Unit
) {
    if (applicationScope.closed) return

    val feature = koinInject<WelcomeFeature>(scope = applicationScope) { parametersOf(title) }

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is WelcomeEvent.HandleFailure -> handleError(event.throwable)

                is WelcomeEvent.OpenProject -> openProject(event.path, event.name)

                is WelcomeEvent.ExitApplication -> exitApplication()
            }
        }
    }

    val fileDialog = koinInject<FileDialog>(scope = applicationScope)

    val scope = rememberCoroutineScope()

    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center), size = DpSize(width = 768.dp, height = 512.dp)
    )

    Window(
        onCloseRequest = {
            scope.launch {
                feature.execute(WelcomeCommand.ExitApplication)
            }
        }, state = windowState, title = title, icon = icon, undecorated = true, transparent = true, resizable = false
    ) {
        WindowDraggableArea(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(.3f).fillMaxHeight().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
                    ) {
                        DistortionLogo(
                            title = state.title,
                            logoFont = logoFont,
                            monoFont = monoFont,
                            textColor = MaterialTheme.colorScheme.onSurface
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            ActionButton(text = "Open Folder", icon = Icons.Default.FolderOpen) {
                                scope.launch {
                                    fileDialog.pickDirectory(title = "Open project")?.let { path ->
                                        feature.execute(WelcomeCommand.OpenProject(path = path, name = null))
                                    }
                                }
                            }

                            ActionButton(text = "Exit", onClick = {
                                scope.launch {
                                    feature.execute(WelcomeCommand.ExitApplication)
                                }
                            })
                        }
                    }

                    Box(
                        modifier = Modifier.weight(.6f).fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            state.recentProjects.isEmpty() -> Text(
                                text = "No recent projects",
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )

                            else -> LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                item {
                                    Text(
                                        text = "Recent projects",
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                items(
                                    items = state.recentProjects,
                                    key = RecentProject::path,
                                    contentType = { it::class }) { recentProject ->
                                    RecentProjectItem(recentProject = recentProject, openProject = {
                                        scope.launch {
                                            feature.execute(
                                                WelcomeCommand.OpenProject(
                                                    path = recentProject.path, name = recentProject.name
                                                )
                                            )
                                        }
                                    }, removeFromHistory = {
                                        scope.launch {
                                            feature.execute(WelcomeCommand.RemoveRecentProject(path = recentProject.path))
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}