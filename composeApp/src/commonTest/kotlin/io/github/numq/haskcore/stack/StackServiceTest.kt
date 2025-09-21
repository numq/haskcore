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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.time.Duration

class StackServiceTest {
    private val fileSystemService = mockk<FileSystemService>()
    private val processService = mockk<ProcessService>()
    private val service = StackService.Default(fileSystemService, processService)

    @Test
    fun `hasStack returns true when stack exists`() = runTest {
        coEvery { processService.execute(any(), ".") } returns Result.success(
            ProcessOutput("Version 2.11.1", 0, Duration.ZERO)
        )
        val result = service.hasStack().getOrThrow()
        assertTrue(result)
    }

    @Test
    fun `hasStack returns false when stack not found`() = runTest {
        coEvery { processService.execute(any(), ".") } returns Result.success(
            ProcessOutput("not found", 127, Duration.ZERO)
        )
        val result = service.hasStack().getOrThrow()
        assertFalse(result)
    }

    @Test
    fun `getStackVersion parses version`() = runTest {
        coEvery { processService.execute(any(), ".") } returns Result.success(
            ProcessOutput("Version 2.9.3", 0, Duration.ZERO)
        )
        val version = service.getStackVersion().getOrThrow()
        assertEquals("2.9.3", version)
    }

    @Test
    fun `getStackVersion returns null if regex fails`() = runTest {
        coEvery { processService.execute(any(), ".") } returns Result.success(
            ProcessOutput("garbage", 0, Duration.ZERO)
        )
        val version = service.getStackVersion().getOrThrow()
        assertNull(version)
    }

    @Test
    fun `getGhcVersion parses ghc version`() = runTest {
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("version 9.2.5", 0, Duration.ZERO)
        )
        val result = service.getGhcVersion("proj").getOrThrow()
        assertEquals("9.2.5", result)
    }

    @Test
    fun `getGhcVersion returns null if output malformed`() = runTest {
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("no version", 0, Duration.ZERO)
        )
        val result = service.getGhcVersion("proj").getOrThrow()
        assertNull(result)
    }

    @Test
    fun `getResolver extracts resolver`() = runTest {
        every { fileSystemService.exists("proj") } returns Result.success(true)
        every { fileSystemService.isDirectory("proj") } returns Result.success(true)
        every { fileSystemService.exists("proj/stack.yaml") } returns Result.success(true)
        every { fileSystemService.readLines("proj/stack.yaml") } returns Result.success(listOf("resolver: lts-20.10"))
        val resolver = service.getResolver("proj").getOrThrow()
        assertEquals("lts-20.10", resolver)
    }

    @Test
    fun `getResolver returns null if no stack yaml`() = runTest {
        every { fileSystemService.exists("proj") } returns Result.success(true)
        every { fileSystemService.isDirectory("proj") } returns Result.success(true)
        every { fileSystemService.exists("proj/stack.yaml") } returns Result.success(false)
        val resolver = service.getResolver("proj").getOrThrow()
        assertNull(resolver)
    }

    @Test
    fun `getProject returns None if path missing`() = runTest {
        every { fileSystemService.exists("proj") } returns Result.success(false)
        every { fileSystemService.isDirectory("proj") } returns Result.success(false)
        val project = service.getProject("proj").getOrThrow()
        assertIs<StackProject.None>(project)
        assertEquals("unknown", project.name)
    }

    @Test
    fun `getProject returns Incomplete if stack yaml missing`() = runTest {
        every { fileSystemService.exists("proj") } returns Result.success(true)
        every { fileSystemService.isDirectory("proj") } returns Result.success(true)
        every { fileSystemService.exists("proj/stack.yaml") } returns Result.success(false)
        val project = service.getProject("proj").getOrThrow()
        assertIs<StackProject.Incomplete>(project)
        assertEquals("proj", project.name)
        assertTrue(StackProject.Requirement.STACK in project.missingRequirements)
    }

    @Test
    fun `getProject returns Incomplete with missing requirements`() = runTest {
        every { fileSystemService.exists("proj") } returns Result.success(true)
        every { fileSystemService.isDirectory("proj") } returns Result.success(true)
        every { fileSystemService.exists("proj/stack.yaml") } returns Result.success(true)
        every { fileSystemService.readLines("proj/stack.yaml") } returns Result.success(listOf("other: value"))
        every { fileSystemService.exists("proj/package.yaml") } returns Result.success(false)
        every { fileSystemService.listDirectory("proj", false) } returns Result.success(emptyList())
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("garbage output", 0, Duration.ZERO)
        )

        val project = service.getProject("proj").getOrThrow()
        assertIs<StackProject.Incomplete>(project)
        assertEquals("unknown", project.name)
        assertTrue("RESOLVER should be missing", StackProject.Requirement.RESOLVER in project.missingRequirements)
        assertTrue("GHC_VERSION should be missing", StackProject.Requirement.GHC_VERSION in project.missingRequirements)
    }

    @Test
    fun `getProject returns Invalid if parsing errors occur`() = runTest {
        every { fileSystemService.exists("proj") } returns Result.success(true)
        every { fileSystemService.isDirectory("proj") } returns Result.success(true)
        every { fileSystemService.exists("proj/stack.yaml") } returns Result.success(true)
        every { fileSystemService.readLines("proj/stack.yaml") } returns Result.failure(IllegalStateException("Parse error"))
        coEvery { processService.execute(any(), "proj") } returns Result.success(
            ProcessOutput("version 9.2.5", 0, Duration.ZERO)
        )

        val project = service.getProject("proj").getOrThrow()
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
        val flow = service.createProject("foo", "proj", StackTemplate.Simple).getOrThrow()
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
        val flow = service.buildProject("proj").getOrThrow()
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
        val flow = service.runProject("proj").getOrThrow()
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
        val flow = service.testProject("proj").getOrThrow()
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
        val flow = service.cleanProject("proj").getOrThrow()
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
        val flow = service.getDependencies("proj").getOrThrow()
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
        val flow = service.addDependency("proj", "text").getOrThrow()
        val outputs = flow.toList()
        assertIs<StackBuildOutput.Progress>(outputs[0])
        assertIs<StackBuildOutput.Completion>(outputs[1])
    }
}