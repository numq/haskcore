package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.syntax.FoldingRegion
import io.github.numq.haskcore.feature.editor.core.syntax.Syntax
import io.github.numq.haskcore.feature.editor.core.toOccurrence
import io.github.numq.haskcore.feature.editor.core.toToken
import io.github.numq.haskcore.service.logger.LoggerService
import io.github.numq.haskcore.service.syntax.SyntaxService
import io.github.numq.haskcore.service.syntax.occurrence.SyntaxOccurrence
import io.github.numq.haskcore.service.syntax.token.SyntaxToken
import io.github.numq.haskcore.service.text.TextService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ObserveSyntax(
    private val editorService: EditorService,
    private val syntaxService: SyntaxService,
    private val loggerService: LoggerService,
    private val textService: TextService,
) : UseCase.Exchange<ObserveSyntax.Input, Flow<Syntax?>> {
    data class Input(val language: Language)

    private companion object {
        const val HIGHLIGHTING_LINE_PADDING_START = 10

        const val HIGHLIGHTING_LINE_PADDING_END = HIGHLIGHTING_LINE_PADDING_START + 1

        const val SYNTAX_HIGHLIGHTING_DELAY_MILLIS = 300L

        const val RETRY_DELAY_MS = 100L
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeSyntax() {
        var lastRevision: Long? = null

        combine(
            flow = textService.snapshot.filterNotNull(),
            flow2 = textService.edits.onStart { emit(null) },
            flow3 = editorService.activeLines,
            transform = { snapshot, edit, activeLines ->
                Triple(snapshot, edit, activeLines)
            }).flatMapLatest { (snapshot, edit, activeLines) ->
            flow {
                try {
                    val needsFullParse =
                        lastRevision == null || syntaxService.syntax.value?.revision != snapshot.revision

                    if (needsFullParse) {
                        syntaxService.fullParse(snapshot = snapshot).getOrElse { throwable ->
                            lastRevision = null

                            throw throwable
                        }

                        lastRevision = snapshot.revision.value
                    }

                    val startLine =
                        (activeLines.start - HIGHLIGHTING_LINE_PADDING_START).coerceIn(0, snapshot.lines - 1)
                    val endLine =
                        (activeLines.endInclusive + HIGHLIGHTING_LINE_PADDING_END).coerceIn(0, snapshot.lines - 1)

                    val startPosition = TextPosition(line = startLine, column = 0)

                    val endPosition = TextPosition(
                        line = endLine, column = snapshot.getLineText(line = endLine).length
                    )

                    val range = TextRange(start = startPosition, end = endPosition)

                    edit?.data?.let { data ->
                        syntaxService.applyChange(
                            snapshot = snapshot, data = data, range = range
                        ).getOrElse { throwable ->
                            if (throwable.message?.contains("closed") == true) {
                                lastRevision = null
                            }

                            throw throwable
                        }
                    }

                    syntaxService.parseFoldingRegions(snapshot = snapshot, range = range).getOrElse { throwable ->
                        println("Parse folding failed: $throwable") // todo
                    }

                    syntaxService.parseTokensPerLine(snapshot = snapshot, range = range).getOrElse { throwable ->
                        println("Parse tokens failed: $throwable") // todo
                    }

                    emit(Unit)
                } catch (throwable: Throwable) {
                    delay(RETRY_DELAY_MS)

                    lastRevision = null

                    emit(Unit)
                }
            }.retryWhen { cause, attempt ->
                delay(RETRY_DELAY_MS * (attempt + 1))

                true
            }
        }.collect()
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeOccurrences() = editorService.caret.map { caret ->
        caret.position
    }.distinctUntilChanged().debounce(SYNTAX_HIGHLIGHTING_DELAY_MILLIS).collect { position ->
        syntaxService.parseOccurrences(position = position).getOrElse { throwable ->
            println("Parse occurrences failed: $throwable") // todo
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override suspend fun Raise<Throwable>.exchange(input: Input) = when (input.language) {
        is Language.Haskell -> channelFlow<Syntax> {
            launch {
                observeSyntax()
            }

            launch {
                observeOccurrences()
            }

            syntaxService.syntax.filterNotNull().distinctUntilChanged().map { syntax ->
                syntax.run {
                    Syntax(
                        revision = revision,
                        foldingRegions = foldingRegions.map { foldingRegion ->
                            FoldingRegion.Expanded(range = foldingRegion.range)
                        },
                        occurrences = occurrences.map(SyntaxOccurrence::toOccurrence),
                        tokensPerLine = tokensPerLine.mapValues { (_, tokens) ->
                            tokens.map(SyntaxToken::toToken)
                        })
                }
            }.collect(::send)
        }

        else -> flowOf(null)
    }
}