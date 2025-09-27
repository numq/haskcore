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
import kotlin.test.*
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class StackServiceTest {
    private lateinit var fileSystemService: FileSystemService
    private lateinit var processService: ProcessService
    private lateinit var stackService: StackService

    @BeforeEach
    fun setup() {
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
    fun `hasStack returns true when stack exists`() = runTest {
        coEvery { processService.execute(any(), ".") } returns Result.success(
            ProcessOutput("Version 2.11.1", 0, Duration.ZERO)
        )
        val result = stackService.hasStack().getOrThrow()
        assertTrue(result)
    }

    @Test
    fun `hasStack returns false when stack not found`() = runTest {
        coEvery { processService.execute(any(), ".") } returns Result.success(
            ProcessOutput("not found", 127, Duration.ZERO)
        )
        val result = stackService.hasStack().getOrThrow()
        assertFalse(result)
    }

    @Test
    fun `getStackVersion parses version`() = runTest {
        coEvery { processService.execute(any(), ".") } returns Result.success(
            ProcessOutput("Version 2.9.3", 0, Duration.ZERO)
        )
        val version = stackService.getStackVersion().getOrThrow()
        assertEquals("2.9.3", version)
    }

    @Test
    fun `getStackVersion returns null if regex fails`() = runTest {
        coEvery { processService.execute(any(), ".") } returns Result.success(
            ProcessOutput("garbage", 0, Duration.ZERO)
        )
        val version = stackService.getStackVersion().getOrThrow()
        assertNull(version)
    }

    @Test
    fun `getGhcVersion parses ghc version`() = runTest {
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("version 9.2.5", 0, Duration.ZERO)
        )
        val result = stackService.getGhcVersion("proj").getOrThrow()
        assertEquals("9.2.5", result)
    }

    @Test
    fun `getGhcVersion returns null if output malformed`() = runTest {
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("no version", 0, Duration.ZERO)
        )
        val result = stackService.getGhcVersion("proj").getOrThrow()
        assertNull(result)
    }

    @Test
    fun `getResolver extracts resolver`() = runTest {
        val stackYamlPath = Path("proj", "stack.yaml").absolutePathString()

        coEvery { fileSystemService.exists("proj") } returns Result.success(true)
        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(true)
        coEvery { fileSystemService.exists(stackYamlPath) } returns Result.success(true)
        coEvery { fileSystemService.readLines(stackYamlPath) } returns Result.success(listOf("resolver: lts-20.10"))
        val resolver = stackService.getResolver("proj").getOrThrow()
        assertEquals("lts-20.10", resolver)
    }

    @Test
    fun `getResolver returns null if no stack yaml`() = runTest {
        val stackYamlPath = Path("proj", "stack.yaml").absolutePathString()

        coEvery { fileSystemService.exists("proj") } returns Result.success(true)
        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(true)
        coEvery { fileSystemService.exists(stackYamlPath) } returns Result.success(false)
        val resolver = stackService.getResolver("proj").getOrThrow()
        assertNull(resolver)
    }

    @Test
    fun `getProject returns None if path missing`() = runTest {
        coEvery { fileSystemService.exists("proj") } returns Result.success(false)
        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(false)
        val project = stackService.getProject("proj").getOrThrow()
        assertIs<StackProject.None>(project)
        assertEquals("unknown", project.name)
    }

    @Test
    fun `getProject returns Incomplete if stack yaml missing`() = runTest {
        val stackYamlPath = Path("proj", "stack.yaml").absolutePathString()

        coEvery { fileSystemService.exists("proj") } returns Result.success(true)
        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(true)
        coEvery { fileSystemService.exists(stackYamlPath) } returns Result.success(false)
        val project = stackService.getProject("proj").getOrThrow()
        assertIs<StackProject.Incomplete>(project)
        assertEquals("proj", project.name)
        assertTrue(StackProject.Requirement.STACK in project.missingRequirements)
    }

    @Test
    fun `getProject returns Incomplete with missing requirements`() = runTest {
        val stackYamlPath = Path("proj", "stack.yaml").absolutePathString()
        val packageYamlPath = Path("proj", "package.yaml").absolutePathString()

        coEvery { fileSystemService.exists("proj") } returns Result.success(true)
        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(true)
        coEvery { fileSystemService.exists(stackYamlPath) } returns Result.success(true)
        coEvery { fileSystemService.readLines(stackYamlPath) } returns Result.success(listOf("other: value"))
        coEvery { fileSystemService.exists(packageYamlPath) } returns Result.success(false)
        coEvery { fileSystemService.listDirectory("proj") } returns Result.success(emptyList())
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("garbage output", 0, Duration.ZERO)
        )

        val project = stackService.getProject("proj").getOrThrow()
        assertIs<StackProject.Incomplete>(project)
        assertEquals("unknown", project.name)
        assertTrue("RESOLVER should be missing", StackProject.Requirement.RESOLVER in project.missingRequirements)
        assertTrue("GHC_VERSION should be missing", StackProject.Requirement.GHC_VERSION in project.missingRequirements)
    }

    @Test
    fun `getProject returns Invalid if parsing errors occur`() = runTest {
        val stackYamlPath = Path("proj", "stack.yaml").absolutePathString()

        coEvery { fileSystemService.exists("proj") } returns Result.success(true)
        coEvery { fileSystemService.isDirectory("proj") } returns Result.success(true)
        coEvery { fileSystemService.exists(stackYamlPath) } returns Result.success(true)
        coEvery { fileSystemService.readLines(stackYamlPath) } returns Result.failure(IllegalStateException("Parse error"))
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("version 9.2.5", 0, Duration.ZERO)
        )

        val project = stackService.getProject("proj").getOrThrow()
        assertIs<StackProject.Invalid>(project)
        assertEquals("unknown", project.name)
        assertTrue(project.errors.isNotEmpty())
    }

    @Test
    fun `createProject streams build outputs`() = runTest {
        coEvery { processService.stream(any(), "proj", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Building foo"),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )
        val flow = stackService.createProject("foo", "proj", StackTemplate.Simple).getOrThrow()
        val outputs = flow.toList()
        assertIs<StackBuildOutput.Progress>(outputs[0])
        assertIs<StackBuildOutput.Completion>(outputs[1])
    }

    @Test
    fun `buildProject streams build outputs`() = runTest {
        coEvery { processService.stream(any(), "proj", any()) } returns Result.success(
            flowOf(
                ProcessOutputChunk.Line("Building bar"),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
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
                ProcessOutputChunk.Line("Hello"),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
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
                ProcessOutputChunk.Line("PASS: Spec1"),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
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
                ProcessOutputChunk.Line("Cleaning"),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
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
                ProcessOutputChunk.Line("aeson-2.0.3.0"),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
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
                ProcessOutputChunk.Line("Building with dependency text"),
                ProcessOutputChunk.Completed(0, Duration.ZERO)
            )
        )
        val flow = stackService.addDependency("proj", "text").getOrThrow()
        val outputs = flow.toList()
        assertIs<StackBuildOutput.Progress>(outputs[0])
        assertIs<StackBuildOutput.Completion>(outputs[1])
    }
}