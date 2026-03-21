package io.github.numq.haskcore.service.lsp.connection

import io.github.numq.haskcore.service.lsp.token.LspTokenLegend
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal sealed interface LspConnectionInternal : AutoCloseable {
    data class Error(val throwable: Throwable) : LspConnectionInternal {
        override fun close() = Unit
    }

    data object Disconnected : LspConnectionInternal {
        override fun close() = Unit
    }

    data object Connecting : LspConnectionInternal {
        override fun close() = Unit
    }

    data class Connected(
        val process: Process, val future: Future<Void>, val server: LanguageServer, val tokenLegend: LspTokenLegend
    ) : LspConnectionInternal {
        private companion object {
            const val SERVER_SHUTDOWN_TIMEOUT_MILLIS = 500L

            const val PROCESS_DESTROY_TIMEOUT_MILLIS = 1500L
        }

        override fun close() {
            runCatching {
                server.shutdown().get(SERVER_SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)

                server.exit()
            }

            future.cancel(true)

            if (process.isAlive) {
                process.destroy()

                if (!process.waitFor(PROCESS_DESTROY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly()
                }
            }
        }
    }
}