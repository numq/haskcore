package io.github.numq.haskcore.service.language.gateway

import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams

internal sealed interface LanguageEvent {
    data class Diagnostics(val params: PublishDiagnosticsParams) : LanguageEvent

    data class Message(val params: MessageParams) : LanguageEvent
}