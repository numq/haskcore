package io.github.numq.haskcore.feature.output.core

import io.github.numq.haskcore.common.core.timestamp.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.*
import kotlin.time.Duration

internal class LocalOutputService(
    private val scope: CoroutineScope, private val outputDataSource: OutputDataSource,
) : OutputService {
    private companion object {
        const val SESSION_LIMIT = 10
    }

    override val output = outputDataSource.outputData.map(OutputData::toOutput).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Output()
    )

    override suspend fun openSession(id: String) = outputDataSource.update { outputData ->
        outputData.copy(activeSession = outputData.sessions.find { sessionData ->
            sessionData.id == id
        })
    }.map {}

    override suspend fun closeSession(id: String) = outputDataSource.update { outputData ->
        val sessions = outputData.sessions

        val currentIndex = sessions.indexOfFirst { sessionData -> sessionData.id == id }

        val updatedSessions = sessions.filterNot { sessionData -> sessionData.id == id }

        val newSelectedSession = when {
            id != outputData.activeSession?.id -> outputData.activeSession

            else -> updatedSessions.getOrNull(currentIndex) ?: updatedSessions.getOrNull(currentIndex - 1)
        }

        outputData.copy(sessions = updatedSessions, activeSession = newSelectedSession)
    }.map {}

    override suspend fun startSession(
        id: String, name: String, configuration: String,
    ) = outputDataSource.update { outputData ->
        val systemLine = OutputLineData.System(
            id = UUID.randomUUID().toString(),
            text = "Process started: $name",
            timestampNanos = Timestamp.now().nanoseconds
        )

        when {
            outputData.sessions.any { sessionData ->
                sessionData.id == id
            } -> outputData

            else -> {
                val filteredSessions = outputData.sessions.filterNot { sessionData ->
                    sessionData.id == id
                }

                val newSession = OutputSessionData.Active(
                    id = id, name = name, configuration = configuration, lines = listOf(systemLine)
                )

                val sessions = (filteredSessions + newSession).take(SESSION_LIMIT)

                outputData.copy(sessions = sessions, activeSession = newSession)
            }
        }
    }.map {}

    override suspend fun stopSession(
        id: String, exitCode: Int, duration: Duration,
    ) = outputDataSource.update { outputData ->
        var activeSession: OutputSessionData? = null

        val sessions = outputData.sessions.map { sessionData ->
            when {
                (sessionData as? OutputSessionData.Active)?.id == id -> {
                    val systemLine = OutputLineData.System(
                        id = UUID.randomUUID().toString(),
                        text = "Process finished with exit code $exitCode (duration: ${duration})",
                        timestampNanos = Timestamp.now().nanoseconds
                    )

                    val completedSession = OutputSessionData.Completed(
                        id = id,
                        name = sessionData.name,
                        configuration = sessionData.configuration,
                        lines = sessionData.lines + systemLine,
                        exitCode = exitCode,
                        durationNanos = duration.inWholeNanoseconds
                    )

                    if (outputData.activeSession?.id == id) {
                        activeSession = completedSession
                    }

                    completedSession
                }

                else -> sessionData
            }
        }

        outputData.copy(sessions = sessions, activeSession = activeSession ?: outputData.activeSession)
    }.map {}

    override suspend fun push(id: String, line: OutputLine) = outputDataSource.update { outputData ->
        var activeSession: OutputSessionData? = null

        val sessions = outputData.sessions.map { session ->
            when {
                (session as? OutputSessionData.Active)?.id == id -> {
                    val newSession = session.copy(lines = session.lines + line.toOutputLineData())

                    if (outputData.activeSession?.id == id) {
                        activeSession = newSession
                    }

                    newSession
                }

                else -> session
            }
        }

        outputData.copy(sessions = sessions, activeSession = activeSession ?: outputData.activeSession)
    }.map {}

    override fun close() {
        scope.cancel()
    }
}