package io.github.numq.haskcore.service.toolchain

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test

internal class LocalBinaryResolverTest {
    private val resolver = LocalBinaryResolver()

    @Test
    fun `should find binary in provided paths`() = runTest {
        val tempDir = Files.createTempDirectory("test_bin")
        val binName = if (System.getProperty("os.name").lowercase().contains("win")) "ghc.exe" else "ghc"
        val binFile = tempDir.resolve(binName).toFile()
        binFile.createNewFile()
        binFile.setExecutable(true)

        val result = resolver.findBinary("ghc", tempDir.toString())

        assert(result.getOrNull() == binFile.absolutePath)
    }
}