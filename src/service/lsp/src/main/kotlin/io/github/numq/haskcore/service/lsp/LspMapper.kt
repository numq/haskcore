package io.github.numq.haskcore.service.lsp

import arrow.core.identity
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.lsp.completion.LspCompletion
import io.github.numq.haskcore.service.lsp.connection.LspConnection
import io.github.numq.haskcore.service.lsp.connection.LspConnectionInternal
import io.github.numq.haskcore.service.lsp.diagnostic.LspDiagnostic
import io.github.numq.haskcore.service.lsp.message.LspMessage
import org.eclipse.lsp4j.*
import io.github.numq.haskcore.core.text.TextEdit as Edit

internal fun Edit.Data.Single.toTextDocumentContentChangeEvent() = TextDocumentContentChangeEvent().apply {
    range = Range(
        Position(startPosition.line, startPosition.column), Position(oldEndPosition.line, oldEndPosition.column)
    )

    text = when (this@toTextDocumentContentChangeEvent) {
        is Edit.Data.Single.Insert -> insertedText

        is Edit.Data.Single.Replace -> newText

        is Edit.Data.Single.Delete -> ""
    }
}

internal fun Range.toTextRange() = TextRange(
    start = TextPosition(line = start.line, column = start.character),
    end = TextPosition(line = end.line, column = end.character)
)

internal fun Location.toTextRange() = range.toTextRange()

internal fun CompletionItem.toLspCompletion() = LspCompletion(
    label = label,
    kind = kind?.let(CompletionItemKind::getValue)?.let { value ->
        LspCompletion.Kind.entries.getOrNull(value - 1)
    } ?: LspCompletion.Kind.VARIABLE,
    detail = detail,
    documentation = documentation?.map(::identity, MarkupContent::getValue),
    insertText = insertText ?: label,
    textEditRange = textEdit?.map(TextEdit::getRange, InsertReplaceEdit::getInsert)?.let(Range::toTextRange),
    sortText = sortText,
    filterText = filterText)

internal fun LspConnectionInternal.toLspConnection() = when (this) {
    is LspConnectionInternal.Error -> LspConnection.Error(throwable = throwable)

    is LspConnectionInternal.Disconnected -> LspConnection.Disconnected

    is LspConnectionInternal.Connecting -> LspConnection.Connecting

    is LspConnectionInternal.Connected -> LspConnection.Connected
}

internal fun Diagnostic.toLspDiagnostic(path: String): LspDiagnostic {
    val range = range.toTextRange()

    val code = code?.get()?.toString()

    val source = source

    val message = message

    return when (severity) {
        DiagnosticSeverity.Error -> LspDiagnostic.Error(
            path = path, range = range, code = code, source = source, message = message
        )

        DiagnosticSeverity.Warning -> LspDiagnostic.Warning(
            path = path, range = range, code = code, source = source, message = message
        )

        DiagnosticSeverity.Information -> LspDiagnostic.Information(
            path = path, range = range, code = code, source = source, message = message
        )

        DiagnosticSeverity.Hint -> LspDiagnostic.Hint(
            path = path, range = range, code = code, source = source, message = message
        )

        else -> LspDiagnostic.Unknown(path = path, range = range, code = code, source = source, message = message)
    }
}

internal fun MessageParams.toLspMessage() = when (type) {
    MessageType.Error -> LspMessage.Error(content = message)

    MessageType.Warning -> LspMessage.Warning(content = message)

    MessageType.Info -> LspMessage.Info(content = message)

    else -> LspMessage.Log(content = message)
}