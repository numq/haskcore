package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.text.TextSnapshot
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.*
import io.github.numq.haskcore.feature.editor.core.analysis.Analysis
import io.github.numq.haskcore.service.logger.LoggerService
import io.github.numq.haskcore.service.lsp.LspService
import io.github.numq.haskcore.service.lsp.completion.LspCompletion
import io.github.numq.haskcore.service.lsp.diagnostic.LspDiagnostic
import io.github.numq.haskcore.service.lsp.hover.LspHover
import io.github.numq.haskcore.service.lsp.token.LspToken
import io.github.numq.haskcore.service.text.TextService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ObserveAnalysis(
    private val editorService: EditorService,
    private val loggerService: LoggerService,
    private val lspService: LspService,
    private val textService: TextService,
) : UseCase.Exchange<ObserveAnalysis.Input, Flow<Analysis?>> {
    data class Input(val path: String, val language: Language)

    private companion object {
        const val HIGHLIGHTING_LINE_PADDING_START = 10

        const val HIGHLIGHTING_LINE_PADDING_END = HIGHLIGHTING_LINE_PADDING_START + 1
    }

    private fun createAnalysisFlow(path: String): Flow<Analysis> {
        val normalizedPath = normalizePath(path = path)

        val currentSnapshot = textService.snapshot.filterNotNull().onStart {
            textService.snapshot.value?.let { snapshot ->
                emit(snapshot)
            }
        }

        val currentFileDiagnostics = lspService.diagnostics.map { diagnostic ->
            diagnostic[normalizedPath].orEmpty()
        }.distinctUntilChanged().onStart {
            emit(emptyList())
        }

        return channelFlow {
            launch { observeLspRequests(path = path).collect() }

            launch { observeEdits(path = path).collect() }

            combine(
                flow = currentSnapshot,
                flow2 = currentFileDiagnostics,
                flow3 = lspService.hover,
                flow4 = lspService.completions,
                flow5 = lspService.tokens
            ) { snapshot, diagnostics, hover, completions, tokens ->
                createAnalysis(
                    snapshot = snapshot,
                    hover = hover,
                    diagnostics = diagnostics,
                    completions = completions,
                    tokens = tokens,
                    normalizedPath = normalizedPath
                )
            }.distinctUntilChanged().collect(::send)
        }
    }

    private fun createAnalysis(
        snapshot: TextSnapshot,
        hover: LspHover?,
        diagnostics: List<LspDiagnostic>,
        completions: List<LspCompletion>,
        tokens: Map<String, List<LspToken>>,
        normalizedPath: String,
    ): Analysis {
        val documentation = hover?.toDocumentation()

        val issues = diagnostics.map(LspDiagnostic::toCodeIssue)

        val suggestions = completions.map(LspCompletion::toCodeSuggestion).distinctBy { suggestion ->
            suggestion.label to suggestion.kind
        }

        val tokensPerLine = tokens[normalizedPath].orEmpty().groupBy { token ->
            token.range.start.line
        }.mapValues { (_, lineTokens) -> lineTokens.map(LspToken::toToken) }

        return Analysis(
            revision = snapshot.revision,
            documentation = documentation,
            issues = issues,
            suggestions = suggestions,
            tokensPerLine = tokensPerLine
        )
    }

    private fun normalizePath(path: String): String = runCatching {
        File(path).canonicalFile.absolutePath
    }.getOrElse {
        File(path).absolutePath
    }

    private fun observeEdits(path: String) = textService.edits.filterNotNull().onEach { edit ->
        lspService.applyEdit(path = path, revision = edit.revision, edit = edit)
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeLspRequests(path: String) = combine(
        textService.snapshot.filterNotNull(), editorService.caret, editorService.activeLines
    ) { snapshot, caret, activeLines ->
        Triple(snapshot, caret, activeLines)
    }.map { (snapshot, caret, activeLines) ->
        coroutineScope {
            launch { lspService.requestCompletions(path = path, position = caret.position) }

            launch { lspService.requestReferences(path = path, position = caret.position) }

            launch {
                val startLine = (activeLines.start - HIGHLIGHTING_LINE_PADDING_START).coerceIn(0, snapshot.lines - 1)

                val endLine = (activeLines.endInclusive + HIGHLIGHTING_LINE_PADDING_END).coerceIn(0, snapshot.lines - 1)

                val range = TextRange(
                    start = TextPosition(line = startLine, column = 0),
                    end = TextPosition(line = endLine, column = snapshot.getLineText(line = endLine).length)

                )
                lspService.requestTokens(path = path, snapshot = snapshot, range = range)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun Raise<Throwable>.exchange(input: Input) = with(input) {
        when (language) {
            is Language.Haskell -> createAnalysisFlow(path = path)

            else -> flowOf(null)
        }
    }
}