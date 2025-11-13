package io.github.numq.haskcore.toolbar.presentation

import io.github.numq.haskcore.feature.Event
import io.github.numq.haskcore.session.Session
import io.github.numq.haskcore.timestamp.Timestamp
import io.github.numq.haskcore.workspace.Workspace
import kotlinx.coroutines.flow.Flow

internal sealed interface ToolbarEvent {
    data class ObserveSession(
        override val flow: Flow<Session>
    ) : ToolbarEvent, Event.Collectable<Session>() {
        override val key = ToolbarEventKey.OBSERVE_SESSION
    }

    data class ObserveWorkspace(
        override val flow: Flow<Workspace>
    ) : ToolbarEvent, Event.Collectable<Workspace>() {
        override val key = ToolbarEventKey.OBSERVE_WORKSPACE
    }

    data object MinimizeWindowRequested : ToolbarEvent, Event {
        override val payload = null

        override val timestamp = Timestamp.now()
    }

    data object ToggleFullscreenRequested : ToolbarEvent, Event {
        override val payload = null

        override val timestamp = Timestamp.now()
    }

    data object ExitApplicationRequested : ToolbarEvent, Event {
        override val payload = null

        override val timestamp = Timestamp.now()
    }
}