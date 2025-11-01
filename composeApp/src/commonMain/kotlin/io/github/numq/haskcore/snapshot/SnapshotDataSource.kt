package io.github.numq.haskcore.snapshot

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import io.github.numq.haskcore.workspace.Workspace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

internal interface SnapshotDataSource {
    val snapshot: Flow<Snapshot>

    suspend fun getCurrentSnapshot(): Result<Snapshot?>

    suspend fun save(snapshot: Snapshot): Result<Unit>

    class Default(private val dataStore: DataStore<Preferences>) : SnapshotDataSource {
        private fun createSnapshot(preferences: Preferences) =
            Snapshot(lastOpenedWorkspacePath = preferences[SnapshotPreferences.Key.LAST_OPENED_WORKSPACE_PATH]?.let { currentWorkspace ->
                Json.decodeFromString<String>(currentWorkspace)
            }, recentWorkspaces = preferences[SnapshotPreferences.Key.RECENT_WORKSPACES]?.let { recentWorkspaces ->
                Json.decodeFromString<List<Workspace>>(recentWorkspaces)
            } ?: emptyList())

        override val snapshot = dataStore.data.map { preferences ->
            Snapshot(lastOpenedWorkspacePath = preferences[SnapshotPreferences.Key.LAST_OPENED_WORKSPACE_PATH]?.let { currentWorkspace ->
                Json.decodeFromString<String>(currentWorkspace)
            }, recentWorkspaces = preferences[SnapshotPreferences.Key.RECENT_WORKSPACES]?.let { recentWorkspaces ->
                Json.decodeFromString<List<Workspace>>(recentWorkspaces)
            } ?: emptyList())
        }

        override suspend fun getCurrentSnapshot() = runCatching {
            dataStore.data.map(::createSnapshot).first()
        }

        override suspend fun save(snapshot: Snapshot) = runCatching {
            dataStore.edit { preferences ->
                when (snapshot.lastOpenedWorkspacePath) {
                    null -> preferences.remove(SnapshotPreferences.Key.LAST_OPENED_WORKSPACE_PATH)

                    else -> preferences[SnapshotPreferences.Key.LAST_OPENED_WORKSPACE_PATH] =
                        Json.encodeToString(snapshot.lastOpenedWorkspacePath)
                }

                preferences[SnapshotPreferences.Key.RECENT_WORKSPACES] = Json.encodeToString(snapshot.recentWorkspaces)
            }

            Unit
        }
    }
}