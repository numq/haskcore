package io.github.numq.haskcore.core.log

import io.github.numq.haskcore.core.timestamp.Timestamp

sealed interface Log {
    val projectId: String?

    val message: String

    val timestamp: Timestamp

    data class Info(override val projectId: String?, override val message: String, override val timestamp: Timestamp) :
        Log

    data class Warning(
        override val projectId: String?, override val message: String, override val timestamp: Timestamp
    ) : Log

    sealed interface Error : Log {
        val className: String

        val stackTrace: String

        data class Handled(
            override val projectId: String?,
            override val message: String,
            override val timestamp: Timestamp,
            override val className: String,
            override val stackTrace: String,
        ) : Error

        data class Internal(
            override val projectId: String?,
            override val message: String,
            override val timestamp: Timestamp,
            override val className: String,
            override val stackTrace: String,
        ) : Error

        data class Critical(
            override val projectId: String?,
            override val message: String,
            override val timestamp: Timestamp,
            override val className: String,
            override val stackTrace: String,
        ) : Error
    }
}