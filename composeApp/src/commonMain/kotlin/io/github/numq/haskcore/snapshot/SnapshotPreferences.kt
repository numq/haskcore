package io.github.numq.haskcore.snapshot

import androidx.datastore.preferences.core.stringPreferencesKey

internal object SnapshotPreferences {
    const val NAME = "snapshot.preferences_pb"

    object Key {
        val LAST_OPENED_WORKSPACE_PATH = stringPreferencesKey("last_opened_workspace_path")

        val RECENT_WORKSPACES = stringPreferencesKey("recent_workspaces")
    }
}