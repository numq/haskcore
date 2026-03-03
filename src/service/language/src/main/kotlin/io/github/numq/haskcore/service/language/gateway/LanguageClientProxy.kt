package io.github.numq.haskcore.service.language.gateway

import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

internal class LanguageClientProxy(private val onEvent: (LanguageEvent) -> Unit) : LanguageClient {
    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
        onEvent(LanguageEvent.Diagnostics(params = diagnostics))
    }

    override fun logMessage(message: MessageParams) {
        onEvent(LanguageEvent.Message(params = message))
    }

    override fun showMessage(params: MessageParams) = Unit

    override fun telemetryEvent(obj: Any) = Unit

    override fun showMessageRequest(p: ShowMessageRequestParams): CompletableFuture<MessageActionItem?>? =
        CompletableFuture.completedFuture(null)
}