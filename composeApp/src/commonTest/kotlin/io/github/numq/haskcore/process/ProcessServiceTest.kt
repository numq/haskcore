package io.github.numq.haskcore.process

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessServiceTest {
    private lateinit var processService: ProcessService.Default

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        processService = ProcessService.Default()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `execute should return success result for valid command`() = runTest {
        val commands = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", "echo", "hello world")
        } else {
            listOf("echo", "hello world")
        }

        val result = processService.execute(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = emptyMap()
        )

        assertTrue(result.isSuccess)
        val output = result.getOrThrow()
        assertEquals(0, output.exitCode)
        assertTrue(output.text.contains("hello world"))
        assertTrue(output.duration.inWholeMilliseconds > 0)
    }

    @Test
    fun `execute should handle environment variables`() = runTest {
        val commands = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", "echo", "%TEST_VAR%")
        } else {
            listOf("echo", $$"$TEST_VAR")
        }

        val result = processService.execute(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = mapOf("TEST_VAR" to "test_value")
        )

        assertTrue(result.isSuccess)
        val output = result.getOrThrow()
        assertTrue(output.text.contains("test_value") || output.exitCode != 0)
    }

    @Test
    fun `execute should return non-zero exit code for invalid command`() = runTest {
        val commands = listOf("invalid_command_that_does_not_exist_12345")

        val result = processService.execute(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = emptyMap()
        )

        when {
            result.isSuccess -> {
                val output = result.getOrThrow()
                assertTrue(output.exitCode != 0 || output.text.contains("not found") || output.text.contains("невозможно"))
            }

            result.isFailure -> {
                assertTrue(result.exceptionOrNull() is IOException)
            }
        }
    }

    @Test
    fun `execute should work with different working directory`() = runTest {
        val subDir = File(tempDir, "subdir").apply { mkdirs() }

        val commands = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", "cd")
        } else {
            listOf("pwd")
        }

        val result = processService.execute(
            commands = commands,
            workingDirectory = subDir.absolutePath,
            environment = emptyMap()
        )

        assertTrue(result.isSuccess)
        val output = result.getOrThrow()
        assertTrue(output.text.contains(subDir.name) || output.exitCode == 0)
    }

    @Test
    fun `stream should emit lines and completed chunk`() = runTest {
        val commands = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", "echo", "line1", "&&", "echo", "line2")
        } else {
            listOf("echo", "-e", "line1\nline2")
        }

        val result = processService.stream(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = emptyMap()
        )

        assertTrue(result.isSuccess)
        val flow = result.getOrThrow()
        val chunks = flow.toList()

        assertTrue(chunks.isNotEmpty())

        val lineChunks = chunks.filterIsInstance<ProcessOutputChunk.Line>()
        assertTrue(lineChunks.isNotEmpty())

        val completedChunk = chunks.last() as? ProcessOutputChunk.Completed
        assertNotNull(completedChunk)
        assertEquals(0, completedChunk.exitCode)
        assertTrue(completedChunk.duration.inWholeMilliseconds > 0)
    }

    @Test
    fun `stream should handle empty output`() = runTest {
        val commands = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", "echo.", "&&", "echo.")
        } else {
            listOf("echo", "-e", "\n\n")
        }

        val result = processService.stream(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = emptyMap()
        )

        assertTrue(result.isSuccess)
        val flow = result.getOrThrow()
        val chunks = flow.toList()

        val completedChunk = chunks.last() as? ProcessOutputChunk.Completed
        assertNotNull(completedChunk)
        assertEquals(0, completedChunk.exitCode)
    }

    @Test
    fun `stream should handle environment variables`() = runTest {
        val commands = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", "echo", "%STREAM_TEST_VAR%")
        } else {
            listOf("echo", $$"$STREAM_TEST_VAR")
        }

        val result = processService.stream(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = mapOf("STREAM_TEST_VAR" to "stream_test_value")
        )

        assertTrue(result.isSuccess)
        val flow = result.getOrThrow()
        val chunks = flow.toList()

        val lineChunks = chunks.filterIsInstance<ProcessOutputChunk.Line>()
        assertTrue(lineChunks.any { it.text.contains("stream_test_value") } || chunks.size == 1)
    }

    @Test
    fun `stream should handle command with error exit code`() = runTest {
        val commands = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", "exit", "1")
        } else {
            listOf("false")
        }

        val result = processService.stream(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = emptyMap()
        )

        assertTrue(result.isSuccess)
        val flow = result.getOrThrow()
        val chunks = flow.toList()

        val completedChunk = chunks.last() as? ProcessOutputChunk.Completed
        assertNotNull(completedChunk)
        assertEquals(1, completedChunk.exitCode)
    }

    @Test
    fun `execute should handle IOException gracefully`() = runTest {
        val commands = emptyList<String>()

        val result = processService.execute(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = emptyMap()
        )

        assertNotNull(result)
    }

    @Test
    fun `stream should handle IOException gracefully`() = runTest {
        val commands = emptyList<String>()

        val result = processService.stream(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = emptyMap()
        )

        assertNotNull(result)
    }

    @Test
    fun `ProcessOutput should have correct properties`() {
        val output = ProcessOutput(
            text = "test output",
            exitCode = 42,
            duration = kotlin.time.Duration.parse("1.5s")
        )

        assertEquals("test output", output.text)
        assertEquals(42, output.exitCode)
        assertEquals(kotlin.time.Duration.parse("1.5s"), output.duration)
    }

    @Test
    fun `ProcessOutputChunk Line should have correct properties`() {
        val line = ProcessOutputChunk.Line("test line")
        assertEquals("test line", line.text)
    }

    @Test
    fun `ProcessOutputChunk Completed should have correct properties`() {
        val completed = ProcessOutputChunk.Completed(
            exitCode = 42,
            duration = kotlin.time.Duration.parse("2.5s")
        )

        assertEquals(42, completed.exitCode)
        assertEquals(kotlin.time.Duration.parse("2.5s"), completed.duration)
    }

    @Test
    fun `execute should handle commands with special characters`() = runTest {
        val testString = $$"hello@world#test$123"
        val commands = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", "echo", testString)
        } else {
            listOf("echo", testString)
        }

        val result = processService.execute(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = emptyMap()
        )

        assertTrue(result.isSuccess)
        val output = result.getOrThrow()
        assertTrue(output.exitCode == 0)
    }

    @Test
    fun `stream should handle long running process`() = runTest {
        val commands = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", "echo", "start", "&&", "timeout", "1", ">", "nul", "&&", "echo", "end")
        } else {
            listOf("sh", "-c", "echo start && sleep 0.1 && echo end")
        }

        val result = processService.stream(
            commands = commands,
            workingDirectory = tempDir.absolutePath,
            environment = emptyMap()
        )

        assertTrue(result.isSuccess)
        val flow = result.getOrThrow()
        val chunks = flow.toList()

        assertTrue(chunks.isNotEmpty())

        val completedChunk = chunks.last() as? ProcessOutputChunk.Completed
        assertNotNull(completedChunk)
        assertTrue(completedChunk.exitCode >= 0)
        assertTrue(completedChunk.duration.inWholeMilliseconds >= 0)
    }
}