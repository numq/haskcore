package io.github.numq.haskcore.stack

import io.github.numq.haskcore.filesystem.FileSystemService
import io.github.numq.haskcore.process.ProcessOutput
import io.github.numq.haskcore.process.ProcessOutputChunk
import io.github.numq.haskcore.process.ProcessService
import io.github.numq.haskcore.stack.output.StackBuildOutput
import io.github.numq.haskcore.stack.output.StackDependencyOutput
import io.github.numq.haskcore.stack.output.StackRunOutput
import io.github.numq.haskcore.stack.output.StackTestOutput
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class StackServiceTest {
    private lateinit var fileSystemService: FileSystemService
    private lateinit var processService: ProcessService
    private lateinit var stackService: StackService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        fileSystemService = mockk()
        processService = mockk()
        stackService = StackService.Default(fileSystemService, processService)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getProject returns valid project when all data available`() = runTest {
        val stackYamlPath = Path("proj", "stack.yaml").absolutePathString()
        val packageYamlPath = Path("proj", "package.yaml").absolutePathString()

        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(true)
        coEvery { fileSystemService.exists(stackYamlPath) } returns Result.success(true)
        coEvery { fileSystemService.exists(packageYamlPath) } returns Result.success(true)
        coEvery { fileSystemService.readLines(stackYamlPath) } returns Result.success(listOf("resolver: lts-20.10"))
        coEvery { fileSystemService.readLines(packageYamlPath) } returns Result.success(
            listOf("name: my-project", "dependencies:", "- base", "- text")
        )
        coEvery { fileSystemService.listDirectory("proj") } returns Result.success(emptyList())
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("version 9.2.5", 0, Duration.ZERO)
        )

        val project = stackService.getProject("proj").getOrThrow()

        assertEquals("proj", project.path)
        assertEquals("my-project", project.name)
        assertEquals("lts-20.10", project.resolver)
        assertEquals("9.2.5", project.ghcVersion)
        assertEquals(listOf("base", "text"), project.dependencies)
    }

    @Test
    fun `getProject returns failure for invalid directory`() = runTest {
        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(false)

        val result = stackService.getProject("proj")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid directory") == true)
    }

    @Test
    fun `getProject returns failure when no stack yaml`() = runTest {
        val stackYamlPath = Path("proj", "stack.yaml").absolutePathString()

        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(true)
        coEvery { fileSystemService.exists(stackYamlPath) } returns Result.success(false)

        val result = stackService.getProject("proj")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No stack.yaml found") == true)
    }

    @Test
    fun `getProject returns failure when no resolver`() = runTest {
        val stackYamlPath = Path("proj", "stack.yaml").absolutePathString()
        val packageYamlPath = Path("proj", "package.yaml").absolutePathString()

        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(true)
        coEvery { fileSystemService.exists(stackYamlPath) } returns Result.success(true)
        coEvery { fileSystemService.exists(packageYamlPath) } returns Result.success(true)
        coEvery { fileSystemService.readLines(stackYamlPath) } returns Result.success(listOf("other: value"))
        coEvery { fileSystemService.readLines(packageYamlPath) } returns Result.success(listOf("name: my-project"))
        coEvery { fileSystemService.listDirectory("proj") } returns Result.success(emptyList())
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("version 9.2.5", 0, Duration.ZERO)
        )

        val result = stackService.getProject("proj")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No resolver found") == true)
    }

    @Test
    fun `getProject returns failure when no GHC version`() = runTest {
        val stackYamlPath = Path("proj", "stack.yaml").absolutePathString()
        val packageYamlPath = Path("proj", "package.yaml").absolutePathString()

        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(true)
        coEvery { fileSystemService.exists(stackYamlPath) } returns Result.success(true)
        coEvery { fileSystemService.exists(packageYamlPath) } returns Result.success(true)
        coEvery { fileSystemService.readLines(stackYamlPath) } returns Result.success(listOf("resolver: lts-20.10"))
        coEvery { fileSystemService.readLines(packageYamlPath) } returns Result.success(listOf("name: my-project"))
        coEvery { fileSystemService.listDirectory("proj") } returns Result.success(emptyList())
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("garbage output", 0, Duration.ZERO)
        )

        val result = stackService.getProject("proj")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No GHC found") == true)
    }

    @Test
    fun `getProject extracts name from cabal file when package yaml missing`() = runTest {
        val stackYamlPath = Path("proj", "stack.yaml").absolutePathString()
        val packageYamlPath = Path("proj", "package.yaml").absolutePathString()
        val cabalFile = "my-project.cabal"
        val fullCabalPath = Path("proj", cabalFile).absolutePathString()

        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(true)
        coEvery { fileSystemService.exists(stackYamlPath) } returns Result.success(true)
        coEvery { fileSystemService.exists(packageYamlPath) } returns Result.success(false)
        coEvery { fileSystemService.listDirectory("proj") } returns Result.success(listOf(cabalFile))
        coEvery { fileSystemService.readLines(fullCabalPath) } returns Result.success(listOf("name: my-cabal-project"))
        coEvery { fileSystemService.readLines(stackYamlPath) } returns Result.success(listOf("resolver: lts-20.10"))
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("version 9.2.5", 0, Duration.ZERO)
        )

        val project = stackService.getProject("proj").getOrThrow()
        assertEquals("my-cabal-project", project.name)
    }

    @Test
    fun `buildProject streams build outputs`() = runTest {
        coEvery { processService.stream(any(), "proj", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Building bar"), ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )
        val flow = stackService.buildProject("proj").getOrThrow()
        val outputs = flow.toList()
        assertIs<StackBuildOutput.Progress>(outputs[0])
        assertIs<StackBuildOutput.Completion>(outputs[1])
    }

    @Test
    fun `runProject streams run outputs`() = runTest {
        coEvery { processService.stream(any(), "proj", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Hello"), ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )
        val flow = stackService.runProject("proj").getOrThrow()
        val outputs = flow.toList()
        assertIs<StackRunOutput.Output>(outputs[0])
        assertIs<StackRunOutput.Completion>(outputs[1])
    }

    @Test
    fun `testProject streams test outputs`() = runTest {
        coEvery { processService.stream(any(), "proj", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("PASS: Spec1"), ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )
        val flow = stackService.testProject("proj").getOrThrow()
        val outputs = flow.toList()
        assertIs<StackTestOutput.Result>(outputs[0])
        assertIs<StackTestOutput.Completion>(outputs[1])
    }

    @Test
    fun `cleanProject streams build outputs`() = runTest {
        coEvery { processService.stream(any(), "proj", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Cleaning"), ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )
        val flow = stackService.cleanProject("proj").getOrThrow()
        val outputs = flow.toList()
        assertIs<StackBuildOutput.Progress>(outputs[0])
        assertIs<StackBuildOutput.Completion>(outputs[1])
    }

    @Test
    fun `getDependencies streams dependency outputs`() = runTest {
        coEvery { processService.stream(any(), "proj", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("aeson-2.0.3.0"), ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )
        val flow = stackService.getDependencies("proj").getOrThrow()
        val outputs = flow.toList()
        val dep = outputs[0] as StackDependencyOutput.Info
        assertEquals("aeson", dep.name)
        assertEquals("2.0.3.0", dep.version)
        assertIs<StackDependencyOutput.Completion>(outputs[1])
    }

    @Test
    fun `addDependency streams build outputs`() = runTest {
        coEvery { processService.stream(any(), "proj", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Building with dependency text"), ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )
        val flow = stackService.addDependency("proj", "text").getOrThrow()
        val outputs = flow.toList()
        assertIs<StackBuildOutput.Progress>(outputs[0])
        assertIs<StackBuildOutput.Completion>(outputs[1])
    }
}