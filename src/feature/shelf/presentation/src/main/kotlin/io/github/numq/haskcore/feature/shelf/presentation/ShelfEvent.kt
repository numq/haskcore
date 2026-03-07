package io.github.numq.haskcore.feature.shelf.presentation

internal sealed interface ShelfEvent {
    data class HandleFailure(val throwable: Throwable) : ShelfEvent
}