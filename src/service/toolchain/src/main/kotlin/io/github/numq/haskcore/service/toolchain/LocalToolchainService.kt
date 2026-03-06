package io.github.numq.haskcore.service.toolchain

import arrow.core.Either
import arrow.core.identity
import arrow.core.raise.either
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile
import kotlin.time.Duration.Companion.milliseconds

internal class LocalToolchainService(
    private val scope: CoroutineScope,
    private val binaryResolver: BinaryResolver,
    private val processRunner: ProcessRunner,
    private val toolchainDataSource: ToolchainDataSource,
) : ToolchainService {
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override val toolchain = toolchainDataSource.toolchainData.distinctUntilChanged().debounce(100.milliseconds)
        .flatMapLatest { toolchainData ->
            flow {
                emit(Toolchain.Scanning)

                emit(validateAll(toolchainData = toolchainData))
            }
        }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = Toolchain.Scanning)

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

            else -> {
                val path = Path.of(configuredPath)

                when {
                    path.exists() && path.isRegularFile() && path.isExecutable() -> configuredPath

                    else -> raise(IllegalArgumentException("Path $configuredPath is not an executable file"))
                }
            }
        }

        val version = processRunner.runCommand(path = path, "--numeric-version").bind()

        factory(path, version)
    }

    private suspend fun validateAll(toolchainData: ToolchainData) = either<Throwable, Toolchain> {
        supervisorScope {
            val ghcDeferred = async {
                validateTool(configuredPath = toolchainData.ghcPath, binaryName = "ghc", factory = Tool::Ghc)
            }

            val cabalDeferred = async {
                validateTool(configuredPath = toolchainData.cabalPath, binaryName = "cabal", factory = Tool::Cabal)
            }

            val stackDeferred = async {
                validateTool(configuredPath = toolchainData.stackPath, binaryName = "stack", factory = Tool::Stack)
            }

            val hlsDeferred = async {
                validateTool(
                    configuredPath = toolchainData.hlsPath,
                    binaryName = "haskell-language-server-wrapper",
                    factory = Tool::Hls
                )
            }

            val (ghc, cabal, stack, hls) = awaitAll(ghcDeferred, cabalDeferred, stackDeferred, hlsDeferred)

            val allNotFound = listOf(ghc, cabal, stack, hls).all { tool ->
                tool.isLeft() && tool.leftOrNull() is NoSuchElementException
            }

            when {
                allNotFound -> Toolchain.NotFound

                else -> Toolchain.Detected(ghc = ghc, cabal = cabal, stack = stack, hls = hls)
            }
        }
    }.fold(ifLeft = Toolchain::Error, ifRight = ::identity)

    override suspend fun updatePaths(
        ghcPath: String?, cabalPath: String?, stackPath: String?, hlsPath: String?
    ) = toolchainDataSource.update { toolchainData ->
        toolchainData.copy(
            ghcPath = ghcPath ?: toolchainData.ghcPath,
            cabalPath = cabalPath ?: toolchainData.cabalPath,
            stackPath = stackPath ?: toolchainData.stackPath,
            hlsPath = hlsPath ?: toolchainData.hlsPath
        )
    }.map {}

    override fun close() {
        scope.cancel()
    }
}