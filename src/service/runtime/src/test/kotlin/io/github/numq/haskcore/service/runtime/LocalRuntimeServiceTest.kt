package io.github.numq.haskcore.service.runtime

import arrow.core.getOrElse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LocalRuntimeServiceTest {
    @Test
    fun `should start process and receive output`() = runTest {
        val service = LocalRuntimeService(this)
        val request = RuntimeRequest.Cabal(
            id = "build-1", name = "Build Project", arguments = listOf("--version")
        )

        val flow = service.execute(request).getOrElse { throw it }

        val firstEvent = flow.first()

        assertTrue(firstEvent is RuntimeEvent.Started)
    }

    @Test
    fun `should receive process output`() = runTest {
        val service = LocalRuntimeService(this)
        val request = RuntimeRequest.Cabal("id", "name", listOf("--version"))

        val flow = service.execute(request).getOrElse { throw it }

        val events = flow.toList()

        assertTrue(events.any { it is RuntimeEvent.Stdout })
        assertTrue(events.any { it is RuntimeEvent.Terminated })
    }
}