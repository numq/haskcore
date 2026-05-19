package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.syntax.FoldingRegion
import io.github.numq.haskcore.feature.editor.core.syntax.Occurrence
import io.github.numq.haskcore.feature.editor.core.syntax.Syntax
import io.github.numq.haskcore.feature.editor.core.syntax.Token
import io.github.numq.haskcore.service.logger.LoggerService
import io.github.numq.haskcore.service.syntax.SyntaxService
import io.github.numq.haskcore.service.syntax.occurrence.SyntaxOccurrence
import io.github.numq.haskcore.service.syntax.token.SyntaxToken
import io.github.numq.haskcore.service.text.TextService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ObserveSyntax(
    private val editorService: EditorService,
    private val syntaxService: SyntaxService,
    private val loggerService: LoggerService, // todo
    private val textService: TextService,
) : UseCase.Exchange<ObserveSyntax.Input, Flow<Syntax?>> {
    data class Input(val language: Language)

    private companion object {
        const val HIGHLIGHTING_LINE_PADDING_START = 10

        const val HIGHLIGHTING_LINE_PADDING_END = HIGHLIGHTING_LINE_PADDING_START + 1

        const val SYNTAX_HIGHLIGHTING_DELAY_MILLIS = 300L
    }

    private suspend fun observeSyntax() {
        var fullParseNeeded = true

        combine(
            flow = textService.snapshot,
            flow2 = textService.edits.onStart { emit(null) },
            flow3 = editorService.activeLines,
            transform = { snapshot, edit, activeLines ->
                when (snapshot) {
                    null -> {
                        fullParseNeeded = true

                        null
                    }

                    else -> {
                        if (fullParseNeeded) {
                            this@ObserveSyntax.syntaxService.fullParse(snapshot = snapshot).getOrElse(::println) // todo

                            fullParseNeeded = false
                        }

                        val startLine =
                            (activeLines.start - HIGHLIGHTING_LINE_PADDING_START).coerceIn(0, snapshot.lines - 1)

                        val endLine =
                            (activeLines.endInclusive + HIGHLIGHTING_LINE_PADDING_END).coerceIn(0, snapshot.lines - 1)

                        val startPosition = TextPosition(line = startLine, column = 0)

                        val endPosition =
                            TextPosition(line = endLine, column = snapshot.getLineText(line = endLine).length)

                        val range = TextRange(start = startPosition, end = endPosition)

                        edit?.data?.let { data ->
                            syntaxService.applyChange(
                                snapshot = snapshot, data = data, range = range
                            ).getOrElse(::println) // todo
                        }

                        snapshot to range
                    }
                }
            }).filterNotNull().distinctUntilChanged().conflate().collect { (snapshot, range) ->
            syntaxService.parseFoldingRegions(range = range).getOrElse(::println) // todo

            syntaxService.parseTokensPerLine(snapshot = snapshot, range = range).getOrElse(::println) // todo
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeOccurrences() = editorService.caret.map { caret ->
        caret.position
    }.distinctUntilChanged().debounce(SYNTAX_HIGHLIGHTING_DELAY_MILLIS).collect { position ->
        syntaxService.parseOccurrences(position = position).getOrElse(::println)
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override suspend fun Raise<Throwable>.exchange(input: Input) = when (input.language) {
        is Language.Haskell -> channelFlow {
            launch {
                observeSyntax()
            }

            launch {
                observeOccurrences()
            }

            syntaxService.syntax.map { syntax ->
                syntax?.run {
                    val foldingRegions = foldingRegions.map { foldingRegion ->
                        FoldingRegion.Expanded(range = foldingRegion.range)
                    }

                    val occurrences = occurrences.map { occurrence ->
                        when (occurrence) {
                            is SyntaxOccurrence.Definition -> Occurrence.Definition(range = occurrence.range)

                            is SyntaxOccurrence.Reference -> Occurrence.Reference(range = occurrence.range)
                        }
                    }

                    val tokensPerLine = tokensPerLine.mapValues { (_, tokens) ->
                        tokens.map { token ->
                            val type = when (token.type) {
                                SyntaxToken.Type.KEYWORD -> Token.Type.KEYWORD

                                SyntaxToken.Type.KEYWORD_CONDITIONAL -> Token.Type.KEYWORD_CONDITIONAL

                                SyntaxToken.Type.KEYWORD_IMPORT -> Token.Type.KEYWORD_IMPORT

                                SyntaxToken.Type.KEYWORD_REPEAT -> Token.Type.KEYWORD_REPEAT

                                SyntaxToken.Type.KEYWORD_DIRECTIVE -> Token.Type.KEYWORD_DIRECTIVE

                                SyntaxToken.Type.KEYWORD_EXCEPTION -> Token.Type.KEYWORD_EXCEPTION

                                SyntaxToken.Type.KEYWORD_DEBUG -> Token.Type.KEYWORD_DEBUG

                                SyntaxToken.Type.TYPE -> Token.Type.TYPE

                                SyntaxToken.Type.CONSTRUCTOR -> Token.Type.CONSTRUCTOR

                                SyntaxToken.Type.BOOLEAN -> Token.Type.BOOLEAN

                                SyntaxToken.Type.FUNCTION -> Token.Type.FUNCTION

                                SyntaxToken.Type.FUNCTION_CALL -> Token.Type.FUNCTION_CALL

                                SyntaxToken.Type.VARIABLE -> Token.Type.VARIABLE

                                SyntaxToken.Type.VARIABLE_PARAMETER -> Token.Type.VARIABLE_PARAMETER

                                SyntaxToken.Type.VARIABLE_MEMBER -> Token.Type.VARIABLE_MEMBER

                                SyntaxToken.Type.OPERATOR -> Token.Type.OPERATOR

                                SyntaxToken.Type.NUMBER -> Token.Type.NUMBER

                                SyntaxToken.Type.NUMBER_FLOAT -> Token.Type.NUMBER_FLOAT

                                SyntaxToken.Type.STRING -> Token.Type.STRING

                                SyntaxToken.Type.CHARACTER -> Token.Type.CHARACTER

                                SyntaxToken.Type.STRING_SPECIAL_SYMBOL -> Token.Type.STRING_SPECIAL_SYMBOL

                                SyntaxToken.Type.COMMENT -> Token.Type.COMMENT

                                SyntaxToken.Type.COMMENT_DOCUMENTATION -> Token.Type.COMMENT_DOCUMENTATION

                                SyntaxToken.Type.PUNCTUATION_BRACKET -> Token.Type.PUNCTUATION_BRACKET

                                SyntaxToken.Type.PUNCTUATION_DELIMITER -> Token.Type.PUNCTUATION_DELIMITER

                                SyntaxToken.Type.MODULE -> Token.Type.MODULE

                                SyntaxToken.Type.SPELL -> Token.Type.SPELL

                                SyntaxToken.Type.WILDCARD -> Token.Type.WILDCARD

                                SyntaxToken.Type.LOCAL_DEFINITION -> Token.Type.LOCAL_DEFINITION

                                SyntaxToken.Type.LOCAL_REFERENCE -> Token.Type.LOCAL_REFERENCE

                                SyntaxToken.Type.UNKNOWN -> Token.Type.UNKNOWN
                            }

                            when (token) {
                                is SyntaxToken.Region -> Token.Region(range = token.range, type = type)

                                is SyntaxToken.Atom -> Token.Atom(
                                    range = token.range, type = type, text = token.text
                                )
                            }
                        }
                    }

                    Syntax(
                        revision = revision,
                        foldingRegions = foldingRegions,
                        occurrences = occurrences,
                        tokensPerLine = tokensPerLine
                    )
                }
            }.filterNotNull().filter { syntax ->
                syntax.tokensPerLine.isNotEmpty()
            }.distinctUntilChanged { old, new ->
                old.tokensPerLine == new.tokensPerLine && old.occurrences == new.occurrences
            }.collect(::send)
        }

        else -> flowOf<Syntax?>(null)
    }
}