package io.github.numq.haskcore.service.lsp.diagnostic

import io.github.numq.haskcore.core.text.TextRange

sealed interface LspDiagnostic {
    val path: String

    val range: TextRange

    val code: String?

    val source: String?

    val message: String

    data class Unknown(
        override val path: String,
        override val range: TextRange,
        override val code: String?,
        override val source: String?,
        override val message: String
    ) : LspDiagnostic

    data class Error(
        override val path: String,
        override val range: TextRange,
        override val code: String?,
        override val source: String?,
        override val message: String
    ) : LspDiagnostic

    data class Warning(
        override val path: String,
        override val range: TextRange,
        override val code: String?,
        override val source: String?,
        override val message: String
    ) : LspDiagnostic

    data class Information(
        override val path: String,
        override val range: TextRange,
        override val code: String?,
        override val source: String?,
        override val message: String
    ) : LspDiagnostic

    data class Hint(
        override val path: String,
        override val range: TextRange,
        override val code: String?,
        override val source: String?,
        override val message: String
    ) : LspDiagnostic
}