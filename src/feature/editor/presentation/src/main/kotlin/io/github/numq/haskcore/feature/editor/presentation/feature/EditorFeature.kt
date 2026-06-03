package io.github.numq.haskcore.feature.editor.presentation.feature

import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.presentation.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class EditorFeature(
    path: String,
    language: Language,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    reducer: EditorReducer,
) : Feature<EditorState, EditorCommand, EditorEvent> by Feature(
    initialState = EditorState.Loading,
    scope = scope,
    reducer = reducer,
    EditorCommand.Initialize(path = path, language = language),
)