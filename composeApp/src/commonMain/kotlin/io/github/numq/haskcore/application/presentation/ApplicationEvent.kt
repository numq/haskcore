package io.github.numq.haskcore.application.presentation

import io.github.numq.haskcore.feature.Event
import io.github.numq.haskcore.workspace.Workspace
import kotlinx.coroutines.flow.Flow

internal sealed interface ApplicationEvent {
    data class ObserveWorkspace(
        override val flow: Flow<Workspace>
    ) : ApplicationEvent, Event.Collectable<Workspace>() {
        override val key = ApplicationEventKey.OBSERVE_WORKSPACE
    }
}