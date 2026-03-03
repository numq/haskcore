package io.github.numq.haskcore.service.language.server

import arrow.core.Either
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.Future

internal class HaskellServerProvider(private val client: LanguageClient) : ServerProvider {
    private val _process = atomic<Process?>(null)

    private val _launcherFuture = atomic<Future<Void>?>(null)

    private val _server = MutableStateFlow<LanguageServer?>(null)

    override val server = _server.asStateFlow()

    override suspend fun initialize(hlsPath: String) = Either.catch {
        val process = ProcessBuilder(hlsPath, "--lsp").redirectError(ProcessBuilder.Redirect.INHERIT).start()

        _process.value = process

        val launcher = Launcher.createLauncher(
            client, LanguageServer::class.java, process.inputStream, process.outputStream
        )

        _server.value = launcher.remoteProxy

        _launcherFuture.value = launcher.startListening()
    }

    override fun close() {
        _launcherFuture.value?.cancel(true)

        _process.value?.destroy()

        _server.value = null
    }
}