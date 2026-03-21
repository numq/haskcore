package io.github.numq.haskcore.feature.editor.presentation.feature

import io.github.numq.haskcore.core.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class EditorFeature(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()), reducer: EditorReducer
) : Feature<EditorState, EditorCommand, EditorEvent> by Feature(
    initialState = EditorState.Loading,
    scope = scope,
    reducer = reducer,
    EditorCommand.Initialize,
)