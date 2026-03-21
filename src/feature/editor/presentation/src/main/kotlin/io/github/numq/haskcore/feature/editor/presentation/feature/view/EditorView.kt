package io.github.numq.haskcore.feature.editor.presentation.feature.view

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorEvent
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorFeature
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorState
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

@Composable
fun EditorView(
    projectScope: Scope,
    handleError: (Throwable) -> Unit,
    path: String?,
    font: EditorFont,
    theme: EditorTheme,
    layerFactory: LayerFactory
) {
    if (projectScope.closed) return

    Surface {
        when (path) {
            null -> EditorViewEmpty()

            else -> {
                val koin = getKoin()

                val documentScope = remember(path, projectScope.id) {
                    koin.getOrCreateScope(
                        scopeId = path, qualifier = named<ScopeQualifierType.Document>(), source = path
                    ).apply {
                        linkTo(projectScope)

                        declare(instance = path, qualifier = ScopeQualifier.Document)
                    }
                }

                DisposableEffect(documentScope.id) {
                    onDispose {
                        documentScope.close()
                    }
                }

                key(documentScope.id) {
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

                        is EditorState.Ready -> with(currentState) {
                            EditorViewReady(
                                state = currentState,
                                font = font,
                                theme = theme,
                                layerFactory = layerFactory,
                                execute = feature::execute
                            )
                        }
                    }
                }
            }
        }
    }
}