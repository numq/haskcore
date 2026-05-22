package io.github.numq.haskcore.feature.editor.presentation.feature.view

import androidx.compose.runtime.*
import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.presentation.font.Font
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorEvent
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorFeature
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorState
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun EditorView(
    projectScope: Scope,
    handleError: (Throwable) -> Unit,
    path: String?,
    font: Font,
    theme: EditorTheme,
    layerFactory: LayerFactory,
) {
    when (path) {
        null -> EditorViewEmpty()

        else -> {
            val koin = getKoin()

            val documentScope = remember(projectScope.id, path) {
                val qualifier = ScopeQualifier.Document

                koin.getOrCreateScope(scopeId = path, qualifier = qualifier, source = path).apply {
                    linkTo(projectScope)

                    declare(instance = path, qualifier = qualifier)
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

                    is EditorState.Ready -> EditorViewReady(
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