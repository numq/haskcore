package io.github.numq.haskcore.service.toolchain

import arrow.core.Either
import arrow.core.identity
import arrow.core.raise.either
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import java.io.File

internal class LocalToolchainService(
    private val scope: CoroutineScope,
    private val binaryResolver: BinaryResolver,
    private val processRunner: ProcessRunner,
    private val toolchainDataSource: ToolchainDataSource,
) : ToolchainService {
    private val _toolchain = MutableStateFlow<Toolchain>(Toolchain.Scanning)

    override val toolchain = _toolchain.asStateFlow()

    private suspend fun <T : Tool> validateTool(
        configuredPath: String?, binaryName: String, factory: (path: String, version: String) -> T
    ): Either<Throwable, T> = either {
        val path = when {
            configuredPath == null -> binaryResolver.findBinary(name = binaryName).bind() ?: raise(
                NoSuchElementException("$binaryName not found in PATH")
            )

            File(configuredPath).isDirectory -> binaryResolver.findBinary(
                name = binaryName, configuredPath
            ).bind() ?: raise(NoSuchElementException("$binaryName not found in $configuredPath"))

            else -> configuredPath
        }

        val version = processRunner.runCommand(path = path, "--numeric-version").bind()

        factory(path, version)
    }

    private suspend fun validateAll(data: ToolchainData) = either<Throwable, Toolchain> {
        supervisorScope {
            val ghcDeferred = async {
                validateTool(configuredPath = data.ghcPath, binaryName = "ghc", factory = Tool::Ghc)
            }

            val cabalDeferred = async {
                validateTool(configuredPath = data.cabalPath, binaryName = "cabal", factory = Tool::Cabal)
            }

            val stackDeferred = async {
                validateTool(configuredPath = data.stackPath, binaryName = "stack", factory = Tool::Stack)
            }

            val hlsDeferred = async {
                validateTool(
                    configuredPath = data.hlsPath, binaryName = "haskell-language-server-wrapper", factory = Tool::Hls
                )
            }

            val cabal = cabalDeferred.await()

            val ghc = ghcDeferred.await()

            val stack = stackDeferred.await()

            val hls = hlsDeferred.await()

            val allNotFound = listOf(ghc, cabal, stack, hls).all { tool ->
                tool.isLeft() && tool.leftOrNull() is NoSuchElementException
            }

            when {
                allNotFound -> Toolchain.NotFound

                else -> Toolchain.Detected(ghc = ghc, cabal = cabal, stack = stack, hls = hls)
            }
        }
    }.fold(ifLeft = Toolchain::Error, ifRight = ::identity)

    init {
        scope.launch {
            toolchainDataSource.toolchain.onEach {
                _toolchain.value = Toolchain.Scanning
            }.collectLatest { data ->
                _toolchain.value = validateAll(data = data)
            }
        }
    }

    override suspend fun resetGhcPath() = toolchainDataSource.update { toolchainData ->
        toolchainData.copy(ghcPath = null)
    }.map { }

    override suspend fun resetCabalPath() = toolchainDataSource.update { toolchainData ->
        toolchainData.copy(cabalPath = null)
    }.map { }

    override suspend fun resetStackPath() = toolchainDataSource.update { toolchainData ->
        toolchainData.copy(stackPath = null)
    }.map { }

    override suspend fun resetHlsPath() = toolchainDataSource.update { toolchainData ->
        toolchainData.copy(hlsPath = null)
    }.map { }

    override suspend fun updateGhcPath(path: String) = toolchainDataSource.update { toolchainData ->
        toolchainData.copy(ghcPath = path)
    }.map { }

    override suspend fun updateCabalPath(path: String) = toolchainDataSource.update { toolchainData ->
        toolchainData.copy(cabalPath = path)
    }.map { }

    override suspend fun updateStackPath(path: String) = toolchainDataSource.update { toolchainData ->
        toolchainData.copy(stackPath = path)
    }.map { }

    override suspend fun updateHlsPath(path: String) = toolchainDataSource.update { toolchainData ->
        toolchainData.copy(hlsPath = path)
    }.map { }

    override fun close() {
        scope.cancel()
    }
}