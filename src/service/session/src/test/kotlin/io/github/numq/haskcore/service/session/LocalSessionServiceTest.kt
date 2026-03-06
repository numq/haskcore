package io.github.numq.haskcore.service.session

import arrow.core.Either
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalSessionServiceTest {
    private val sessionDataSource = mockk<SessionDataSource>()
    private val sessionFlow = MutableStateFlow(SessionData())

    @BeforeEach
    fun setup() {
        every { sessionDataSource.sessionData } returns sessionFlow
    }

    @Test
    fun `session flow should map data from source`() = runTest {
        val service = LocalSessionService(backgroundScope, sessionDataSource)
        val testPath = "/test/path"
        val recordData = SessionRecordData(path = testPath, name = "test", timestampNanos = 1000L)

        sessionFlow.value = SessionData(
            history = mapOf(testPath to recordData), active = listOf(recordData)
        )

        advanceUntilIdle()

        val currentSession = service.session.drop(1).first()

        assertEquals(1, currentSession.history.size)
        assertEquals(1, currentSession.active.size)
        assertEquals(testPath, currentSession.history.first().path)
    }

    @Test
    fun `openSessionRecord should update history and active lists`() = runTest {
        val service = LocalSessionService(backgroundScope, sessionDataSource)
        val path = "/path/to/file"
        val name = "file.hs"

        coEvery { sessionDataSource.update(any()) } answers {
            val transform = firstArg<(SessionData) -> SessionData>()
            val updated = transform(SessionData())
            Either.Right(updated)
        }

        val result = service.openSessionRecord(path, name)

        assertTrue(result.isRight())
        coVerify { sessionDataSource.update(any()) }
    }

    @Test
    fun `updateSessionRecord should modify existing record`() = runTest {
        val service = LocalSessionService(backgroundScope, sessionDataSource)
        val path = "/path"
        val newName = "new_name"
        val existingRecord = SessionRecordData(path = path, name = "old", timestampNanos = 1L)

        coEvery { sessionDataSource.update(any()) } answers {
            val transform = firstArg<(SessionData) -> SessionData>()
            val updated = transform(SessionData(history = mapOf(path to existingRecord)))
            Either.Right(updated)
        }

        val result = service.updateSessionRecord(path, newName)

        assertTrue(result.isRight())
        coVerify { sessionDataSource.update(any()) }
    }

    @Test
    fun `closeSessionRecord should remove record from active list`() = runTest {
        val service = LocalSessionService(backgroundScope, sessionDataSource)
        val path = "/active/path"
        val record = SessionRecordData(path = path, name = "active", timestampNanos = 1L)

        coEvery { sessionDataSource.update(any()) } answers {
            val transform = firstArg<(SessionData) -> SessionData>()
            val updated = transform(SessionData(active = listOf(record)))
            assertTrue(updated.active.isEmpty())
            Either.Right(updated)
        }

        val result = service.closeSessionRecord(path)

        assertTrue(result.isRight())
    }

    @Test
    fun `removeSessionRecordFromHistory should remove record from history map`() = runTest {
        val service = LocalSessionService(backgroundScope, sessionDataSource)
        val path = "/history/path"
        val record = SessionRecordData(path = path, name = "history", timestampNanos = 1L)

        coEvery { sessionDataSource.update(any()) } answers {
            val transform = firstArg<(SessionData) -> SessionData>()
            val updated = transform(SessionData(history = mapOf(path to record)))
            assertTrue(updated.history.isEmpty())
            Either.Right(updated)
        }

        val result = service.removeSessionRecordFromHistory(path)

        assertTrue(result.isRight())
    }

    @Test
    fun `history limit should be respected`() = runTest {
        val service = LocalSessionService(backgroundScope, sessionDataSource)
        val pathPrefix = "/path/"
        val initialHistory = (1..30).associate {
            "$pathPrefix$it" to SessionRecordData("$pathPrefix$it", null, it.toLong())
        }

        coEvery { sessionDataSource.update(any()) } answers {
            val transform = firstArg<(SessionData) -> SessionData>()
            val updated = transform(SessionData(history = initialHistory))
            assertEquals(30, updated.history.size)
            Either.Right(updated)
        }

        service.openSessionRecord("/path/new", "new")
    }
}