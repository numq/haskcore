package io.github.numq.haskcore.output

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OutputRepositoryTest {
    private lateinit var repository: OutputRepository

    private val id = "test"

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        repository = OutputRepository.Default()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `observe creates empty list`() = runBlocking {
        val flowResult = repository.observe(id)
        assertTrue(flowResult.isSuccess)

        val list = flowResult.getOrThrow().first()
        assertTrue(list.isEmpty(), "Initial list should be empty")
    }

    @Test
    fun `enqueue adds lines`() = runBlocking {
        repository.observe(id)

        val firstLine = OutputLine(text = "first")
        val secondLine = OutputLine(text = "second")

        repository.enqueue(id, firstLine)
        repository.enqueue(id, secondLine)

        val list = repository.observe(id).getOrThrow().first()
        assertEquals(listOf(firstLine, secondLine), list)
    }

    @Test
    fun `clear empties the log`() = runBlocking {
        repository.observe(id)

        val firstLine = OutputLine(text = "first")
        val secondLine = OutputLine(text = "second")

        repository.enqueue(id, firstLine)
        repository.enqueue(id, secondLine)

        repository.clear(id)

        val list = repository.observe(id).getOrThrow().first()
        assertTrue(list.isEmpty(), "List should be empty after clear")
    }

    @Test
    fun `remove deletes the log`() = runBlocking {
        repository.observe(id)
        repository.enqueue(id, OutputLine(text = "test"))

        repository.remove(id)

        val list = repository.observe(id).getOrThrow().first()
        assertTrue(list.isEmpty(), "Removed log should start empty when observed again")
    }

    @Test
    fun `enqueue on non-observed id does nothing`() = runBlocking {
        val unknownId = "unknown"
        val result = repository.enqueue(unknownId, OutputLine(text = "test"))
        assertTrue(result.isSuccess)

        val list = repository.observe(unknownId).getOrThrow().first()
        assertTrue(list.isEmpty(), "Line should not be added before observe")
    }
}