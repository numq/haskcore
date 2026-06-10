package io.github.numq.haskcore.feature.editor.presentation.feature.view

import androidx.compose.runtime.*
import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.core.text.TextEncoding
import io.github.numq.haskcore.common.core.text.TextLineEnding
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.presentation.font.Font
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorEvent
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorFeature
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorState
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope

@Composable
fun EditorView(
    projectScope: Scope,
    handleError: (Throwable) -> Unit,
    path: String?,
    language: Language?,
    font: Font,
    theme: EditorTheme,
    layerFactory: LayerFactory,
    onTextPosition: (TextPosition?) -> Unit,
    onTextLineEnding: (TextLineEnding?) -> Unit,
    onTextEncoding: (TextEncoding?) -> Unit,
) {
    when {
        path != null && language != null -> {
            val koin = getKoin()

            val documentScope = remember(projectScope.id, path) {
                val projectPath = projectScope.get<String>(qualifier = ScopeQualifier.Project)

                val qualifier = ScopeQualifier.Document

                koin.getOrCreateScope(scopeId = path, qualifier = qualifier, source = path).apply {
                    linkTo(projectScope)

                    declare(instance = projectPath, qualifier = ScopeQualifier.Project)

                    declare(instance = path, qualifier = qualifier)
                }
            }

            DisposableEffect(documentScope.id) {
                onDispose {
                    documentScope.close()
                }
            }

            key(documentScope.id) {
                val feature = koinInject<EditorFeature>(scope = documentScope) {
                    parametersOf(path, language)
                }

                val state by feature.state.collectAsState()

                LaunchedEffect(Unit) {
                    feature.events.collect { event ->
                        when (event) {
                            is EditorEvent.HandleFailure -> handleError(event.throwable)
                        }
                    }
                }

                val position by remember(state) {
                    derivedStateOf {
                        (state as? EditorState.Ready)?.editor?.caret?.position
                    }
                }

                val lineEnding by remember(state) {
                    derivedStateOf {
                        (state as? EditorState.Ready)?.editor?.snapshot?.lineEnding
                    }
                }

                val encoding by remember(state) {
                    derivedStateOf {
                        (state as? EditorState.Ready)?.editor?.snapshot?.encoding
                    }
                }

                LaunchedEffect(position) {
                    onTextPosition(position)
                }

                LaunchedEffect(lineEnding) {
                    onTextLineEnding(lineEnding)
                }

                LaunchedEffect(encoding) {
                    onTextEncoding(encoding)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        onTextPosition(null)
                        onTextLineEnding(null)
                        onTextEncoding(null)
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

        else -> EditorViewEmpty()
    }
}