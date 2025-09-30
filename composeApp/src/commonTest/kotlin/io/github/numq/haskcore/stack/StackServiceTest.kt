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
    fun `createProject streams build outputs for simple template`() = runTest {
        coEvery { processService.stream(any(), "projects", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Downloading template"),
                ProcessOutputChunk.Line("Building project my-project"),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )

        val flow = stackService.createProject(
            name = "my-project",
            path = "projects",
            template = StackTemplate.Simple
        ).getOrThrow()

        val outputs = flow.toList()

        val firstOutput = outputs[0] as StackBuildOutput.Progress
        assertEquals("Downloading template", firstOutput.message)

        val secondOutput = outputs[1] as StackBuildOutput.Progress
        assertEquals("Building project my-project", secondOutput.message)

        val completion = outputs[2] as StackBuildOutput.Completion
        assertEquals(0, completion.exitCode)
    }

    @Test
    fun `createProject streams build outputs for library template`() = runTest {
        coEvery { processService.stream(any(), "libs", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Creating library my-lib"),
                ProcessOutputChunk.Line("Writing files..."),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )

        val flow = stackService.createProject(
            name = "my-lib",
            path = "libs",
            template = StackTemplate.Library
        ).getOrThrow()

        val outputs = flow.toList()
        assertEquals(3, outputs.size)
        assertIs<StackBuildOutput.Progress>(outputs[0])
        assertIs<StackBuildOutput.Progress>(outputs[1])
        assertIs<StackBuildOutput.Completion>(outputs[2])
    }

    @Test
    fun `createProject handles warnings during project creation`() = runTest {
        coEvery { processService.stream(any(), "projects", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Warning: Using default resolver"),
                ProcessOutputChunk.Line("Building project test-project"),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )

        val flow = stackService.createProject(
            name = "test-project",
            path = "projects",
            template = StackTemplate.Executable
        ).getOrThrow()

        val outputs = flow.toList()

        val warning = outputs[0] as StackBuildOutput.Warning
        assertEquals("Warning: Using default resolver", warning.message)

        assertIs<StackBuildOutput.Progress>(outputs[1])
        assertIs<StackBuildOutput.Completion>(outputs[2])
    }

    @Test
    fun `createProject handles errors during project creation`() = runTest {
        coEvery { processService.stream(any(), "projects", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Error: Project name already exists"),
                ProcessOutputChunk.Completed(1, Duration.ZERO)
            )
        )

        val flow = stackService.createProject(
            name = "existing-project",
            path = "projects",
            template = StackTemplate.Simple
        ).getOrThrow()

        val outputs = flow.toList()

        val error = outputs[0] as StackBuildOutput.Error
        assertEquals("Error: Project name already exists", error.message)

        val completion = outputs[1] as StackBuildOutput.Completion
        assertEquals(1, completion.exitCode)
    }

    @Test
    fun `createProject works with all template types`() = runTest {
        val templates = listOf(
            StackTemplate.Simple,
            StackTemplate.Library,
            StackTemplate.Executable,
            StackTemplate.TestSuite,
            StackTemplate.Full
        )

        coEvery { processService.stream(any(), "test", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Project created successfully"),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )

        templates.forEach { template ->
            val flow = stackService.createProject(
                name = "test-project",
                path = "test",
                template = template
            ).getOrThrow()

            val outputs = flow.toList()
            assertIs<StackBuildOutput.Progress>(outputs[0])
            assertIs<StackBuildOutput.Completion>(outputs[1])
        }
    }

    @Test
    fun `createProject returns failure when process service fails`() = runTest {
        coEvery { processService.stream(any(), "projects", any()) } returns Result.failure(
            RuntimeException("Process execution failed")
        )

        val result = stackService.createProject(
            name = "my-project",
            path = "projects",
            template = StackTemplate.Simple
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Process execution failed") == true)
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