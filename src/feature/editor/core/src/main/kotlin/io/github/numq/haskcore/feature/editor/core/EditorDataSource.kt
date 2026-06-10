package io.github.numq.haskcore.feature.editor.core

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface EditorDataSource : AutoCloseable {
    val editorData: Flow<EditorData>

    suspend fun get(): Either<Throwable, EditorData>

    suspend fun update(transform: (EditorData) -> EditorData): Either<Throwable, EditorData>
}