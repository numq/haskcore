package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.analysis.Analysis
import io.github.numq.haskcore.feature.editor.core.analysis.CodeIssue
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion
import io.github.numq.haskcore.service.logger.LoggerService
import io.github.numq.haskcore.service.lsp.LspService
import io.github.numq.haskcore.service.lsp.completion.LspCompletion
import io.github.numq.haskcore.service.lsp.connection.LspConnection
import io.github.numq.haskcore.service.lsp.diagnostic.LspDiagnostic
import io.github.numq.haskcore.service.text.TextService
import io.github.numq.haskcore.service.toolchain.Toolchain
import io.github.numq.haskcore.service.toolchain.ToolchainService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ObserveAnalysis(
    private val editorService: EditorService,
    private val loggerService: LoggerService, // todo
    private val lspService: LspService,
    private val textService: TextService,
    private val toolchainService: ToolchainService,
) : UseCase.Exchange<ObserveAnalysis.Input, Flow<Analysis?>> {
    data class Input(val path: String, val language: Language)

    private companion object {
        const val HIGHLIGHTING_LINE_PADDING_START = 10

        const val HIGHLIGHTING_LINE_PADDING_END = HIGHLIGHTING_LINE_PADDING_START + 1
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeAnalysis(path: String) {
        toolchainService.toolchain.filterIsInstance<Toolchain.Detected>().mapNotNull { toolchain ->
            toolchain.hls.getOrNull()?.path
        }.distinctUntilChanged().flatMapLatest { hlsPath ->
            lspService.start(hlsPath = hlsPath).getOrElse(::println) // todo

            lspService.connection.mapLatest { connection ->
                when (connection) {
                    is LspConnection.Error -> println(connection.throwable) // todo

                    is LspConnection.Connected -> combine(
                        flow = textService.snapshot.filterNotNull(),
                        flow2 = textService.edits.onStart { emit(null) },
                        flow3 = editorService.caret,
                        flow4 = editorService.activeLines,
                        transform = { snapshot, edit, caret, activeLines ->
                            val revision = snapshot.revision

                            val position = caret.position

                            if (edit?.revision == revision) {
                                this@ObserveAnalysis.lspService.applyEdit(
                                    path = path, revision = revision, edit = edit
                                ).getOrElse { throwable ->
                                    println(throwable) // todo
                                }
                            }

                            this@ObserveAnalysis.lspService.requestCompletions(
                                path = path, position = position
                            ).getOrElse { throwable ->
                                println(throwable) // todo
                            }

                            this@ObserveAnalysis.lspService.requestReferences(
                                path = path, position = position
                            ).getOrElse { throwable ->
                                println(throwable) // todo
                            }

                            val startLine = (activeLines.start - HIGHLIGHTING_LINE_PADDING_START).coerceIn(
                                0, snapshot.lines - 1
                            )

                            val endLine = (activeLines.endInclusive + HIGHLIGHTING_LINE_PADDING_END).coerceIn(
                                0, snapshot.lines - 1
                            )

                            val startPosition = TextPosition(line = startLine, column = 0)

                            val endPosition =
                                TextPosition(line = endLine, column = snapshot.getLineText(line = endLine).length)

                            val range = TextRange(start = startPosition, end = endPosition)

                            lspService.requestTokens(
                                path = path, snapshot = snapshot, range = range
                            ).getOrElse(::println) // todo
                        })

                    else -> null
                }
            }
        }.collect()
    }

    override suspend fun Raise<Throwable>.exchange(input: Input) = with(input) {
        when (language) {
            is Language.Haskell -> channelFlow {
                launch {
                    observeAnalysis(path = path)
                }

                combine(
                    flow = textService.snapshot.filterNotNull(),
                    flow2 = lspService.completions,
                    flow3 = lspService.diagnostics,
                    transform = { snapshot, completions, diagnostics ->
                        val issues = diagnostics.map { diagnostic ->
                            with(diagnostic) {
                                when (this) {
                                    is LspDiagnostic.Unknown -> CodeIssue.Unknown(
                                        range = range, message = message, source = source, code = code
                                    )

                                    is LspDiagnostic.Error -> CodeIssue.Error(
                                        range = range, message = message, source = source, code = code
                                    )

                                    is LspDiagnostic.Warning -> CodeIssue.Warning(
                                        range = range, message = message, source = source, code = code
                                    )

                                    is LspDiagnostic.Information -> CodeIssue.Information(
                                        range = range, message = message, source = source, code = code
                                    )

                                    is LspDiagnostic.Hint -> CodeIssue.Hint(
                                        range = range, message = message, source = source, code = code
                                    )
                                }
                            }
                        }

                        val suggestions = completions.map { completion ->
                            with(completion) {
                                val kind = when (kind) {
                                    LspCompletion.Kind.METHOD -> CodeSuggestion.Kind.METHOD

                                    LspCompletion.Kind.FUNCTION -> CodeSuggestion.Kind.FUNCTION

                                    LspCompletion.Kind.CONSTRUCTOR -> CodeSuggestion.Kind.CONSTRUCTOR

                                    LspCompletion.Kind.FIELD -> CodeSuggestion.Kind.FIELD

                                    LspCompletion.Kind.VARIABLE -> CodeSuggestion.Kind.VARIABLE

                                    LspCompletion.Kind.CLASS -> CodeSuggestion.Kind.CLASS

                                    LspCompletion.Kind.INTERFACE -> CodeSuggestion.Kind.INTERFACE

                                    LspCompletion.Kind.MODULE -> CodeSuggestion.Kind.MODULE

                                    LspCompletion.Kind.PROPERTY -> CodeSuggestion.Kind.PROPERTY

                                    LspCompletion.Kind.UNIT -> CodeSuggestion.Kind.UNIT

                                    LspCompletion.Kind.VALUE -> CodeSuggestion.Kind.VALUE

                                    LspCompletion.Kind.ENUM -> CodeSuggestion.Kind.ENUM

                                    LspCompletion.Kind.KEYWORD -> CodeSuggestion.Kind.KEYWORD

                                    LspCompletion.Kind.SNIPPET -> CodeSuggestion.Kind.SNIPPET
                                }

                                CodeSuggestion(
                                    label = label,
                                    kind = kind,
                                    insertText = insertText,
                                    detail = detail,
                                    documentation = documentation,
                                    range = textEditRange
                                )
                            }
                        }

                        Analysis(revision = snapshot.revision, issues = issues, suggestions = suggestions)
                    }).collect(::send)
            }

            else -> flowOf<Analysis?>(null)
        }
    }
}