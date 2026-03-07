package io.github.numq.haskcore.feature.editor.presentation

internal sealed interface EditorEvent {
    data class HandleFailure(val throwable: Throwable) : EditorEvent
}