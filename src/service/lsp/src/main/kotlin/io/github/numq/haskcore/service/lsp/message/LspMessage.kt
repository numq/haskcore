package io.github.numq.haskcore.service.lsp.message

sealed interface LspMessage {
    val content: String

    data class Error(override val content: String) : LspMessage

    data class Warning(override val content: String) : LspMessage

    data class Info(override val content: String) : LspMessage

    data class Log(override val content: String) : LspMessage
}