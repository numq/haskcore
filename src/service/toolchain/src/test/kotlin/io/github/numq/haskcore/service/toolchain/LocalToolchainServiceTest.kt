package io.github.numq.haskcore.service.toolchain

import arrow.core.Either
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalToolchainServiceTest {
    private val binaryResolver = mockk<BinaryResolver>()
    private val processRunner = mockk<ProcessRunner>()
    private val toolchainDataSource = mockk<ToolchainDataSource>()
    private val toolchainDataFlow = MutableSharedFlow<ToolchainData>(replay = 1)

    @Test
    fun `should detect toolchain when all binaries are valid`() = runTest(UnconfinedTestDispatcher()) {
        val path = "/fake/bin/ghc"
        val version = "9.4.5"

        coEvery { toolchainDataSource.toolchain } returns toolchainDataFlow
        coEvery { binaryResolver.findBinary(any(), *anyVararg()) } returns Either.Right(path)
        coEvery { processRunner.runCommand(any(), "--numeric-version") } returns Either.Right(version)

        val service = LocalToolchainService(backgroundScope, binaryResolver, processRunner, toolchainDataSource)

        toolchainDataFlow.emit(
            ToolchainData(
                ghcPath = path, cabalPath = path, stackPath = path, hlsPath = path
            )
        )
        runCurrent()

        val state = service.toolchain.value
        assertTrue(state is Toolchain.Detected)
    }

    @Test
    fun `should transition to Scanning then to NotFound when binaries missing`() = runTest(UnconfinedTestDispatcher()) {
        val processRunner = mockk<ProcessRunner>()
        coEvery { toolchainDataSource.toolchain } returns toolchainDataFlow
        coEvery { binaryResolver.findBinary(any(), any()) } returns Either.Right(null)
        coEvery { binaryResolver.findBinary(any()) } returns Either.Right(null)

        val service = LocalToolchainService(backgroundScope, binaryResolver, processRunner, toolchainDataSource)

        toolchainDataFlow.emit(ToolchainData())
        runCurrent()

        val state = service.toolchain.value

        assertEquals(Toolchain.NotFound, state)
    }

    @Test
    fun `updateGhcPath should trigger dataSource update`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { toolchainDataSource.toolchain } returns toolchainDataFlow
        coEvery { toolchainDataSource.update(any()) } returns Either.Right(ToolchainData(ghcPath = "/new/ghc"))

        val service = LocalToolchainService(backgroundScope, binaryResolver, processRunner, toolchainDataSource)

        val result = service.updateGhcPath("/new/ghc")

        assertTrue(result.isRight())
        coVerify { toolchainDataSource.update(any()) }
    }
}