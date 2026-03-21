package io.github.numq.haskcore.service.lsp.connection

sealed interface LspConnection {
    data class Error(val throwable: Throwable) : LspConnection

    data object Disconnected : LspConnection

    data object Connecting : LspConnection

    data object Connected : LspConnection
}