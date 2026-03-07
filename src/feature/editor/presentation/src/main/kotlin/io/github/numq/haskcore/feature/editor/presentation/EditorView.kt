package io.github.numq.haskcore.feature.editor.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import io.github.numq.haskcore.core.di.ScopePath
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.editor.presentation.layout.LayoutFactory
import io.github.numq.haskcore.feature.presentation.EditorViewReady
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

@Composable
fun EditorView(
    projectScope: Scope, path: String?, font: EditorFont, theme: EditorTheme, handleError: (Throwable) -> Unit
) {
    if (projectScope.closed) return

    Surface {
        when (path) {
            null -> EditorViewEmpty()

            else -> {
                val koin = getKoin()

                val documentScope = remember(path, projectScope.id) {
                    koin.getOrCreateScope(
                        scopeId = path, qualifier = named<ScopeQualifier.Document>(), source = path
                    ).apply {
                        linkTo(projectScope)

                        declare(instance = path, qualifier = ScopePath.Document)
                    }
                }

                DisposableEffect(documentScope.id) {
                    onDispose {
                        documentScope.close()
                    }
                }

                val feature = koinInject<EditorFeature>(scope = documentScope)

                val state by feature.state.collectAsState()

                LaunchedEffect(Unit) {
                    feature.events.collect { event ->
                        when (event) {
                            is EditorEvent.HandleFailure -> handleError(event.throwable)
                        }
                    }
                }

                when (val currentState = state) {
                    is EditorState.Loading -> EditorViewLoading()

                    is EditorState.Ready -> {
                        val layerFactory = koinInject<LayerFactory>(scope = documentScope)

                        val layoutFactory = koinInject<LayoutFactory>(scope = documentScope)

                        with(currentState) {
                            EditorViewReady(
                                snapshot = snapshot,
                                caret = caret,
                                selection = selection,
                                highlighting = highlighting,
                                layerFactory = layerFactory,
                                layoutFactory = layoutFactory,
                                font = font,
                                theme = theme,
                                execute = feature::execute
                            )
                        }
                    }
                }
            }
        }
    }
}