package io.github.numq.haskcore.snapshot

import kotlinx.coroutines.flow.Flow

internal interface SnapshotRepository {
    val snapshot: Flow<Snapshot?>

    suspend fun getCurrentSnapshot(): Result<Snapshot?>

    suspend fun save(snapshot: Snapshot): Result<Unit>

    class Default(private val snapshotDataSource: SnapshotDataSource) : SnapshotRepository {
        override val snapshot = snapshotDataSource.snapshot

        override suspend fun getCurrentSnapshot() = snapshotDataSource.getCurrentSnapshot()

        override suspend fun save(snapshot: Snapshot) = snapshotDataSource.save(snapshot = snapshot)
    }
}