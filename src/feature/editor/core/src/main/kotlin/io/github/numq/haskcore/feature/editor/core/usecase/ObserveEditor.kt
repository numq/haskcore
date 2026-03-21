package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType
import io.github.numq.haskcore.core.text.*
import io.github.numq.haskcore.core.timestamp.Timestamp
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.Editor
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.analysis.Analysis
import io.github.numq.haskcore.feature.editor.core.analysis.CodeIssue
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion
import io.github.numq.haskcore.feature.editor.core.guideline.Guideline
import io.github.numq.haskcore.feature.editor.core.syntax.FoldingRegion
import io.github.numq.haskcore.feature.editor.core.syntax.Occurrence
import io.github.numq.haskcore.feature.editor.core.syntax.Syntax
import io.github.numq.haskcore.feature.editor.core.syntax.Token
import io.github.numq.haskcore.service.document.Document
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.journal.JournalService
import io.github.numq.haskcore.service.logger.LoggerService
import io.github.numq.haskcore.service.lsp.LspService
import io.github.numq.haskcore.service.lsp.completion.LspCompletion
import io.github.numq.haskcore.service.lsp.connection.LspConnection
import io.github.numq.haskcore.service.lsp.diagnostic.LspDiagnostic
import io.github.numq.haskcore.service.syntax.SyntaxService
import io.github.numq.haskcore.service.syntax.occurrence.SyntaxOccurrence
import io.github.numq.haskcore.service.syntax.token.SyntaxToken
import io.github.numq.haskcore.service.text.TextService
import io.github.numq.haskcore.service.toolchain.Toolchain
import io.github.numq.haskcore.service.toolchain.ToolchainService
import io.github.numq.haskcore.service.vfs.VfsService
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ObserveEditor(
    private val documentPath: String,
    private val editorService: EditorService,
    private val documentService: DocumentService,
    private val syntaxService: SyntaxService,
    private val journalService: JournalService,
    private val loggerService: LoggerService, // todo
    private val lspService: LspService,
    private val textService: TextService,
    private val toolchainService: ToolchainService,
    private val vfsService: VfsService,
) : UseCase<Unit, Flow<Editor>> {
    private companion object {
        const val AUTO_SAVE_SAMPLE_MILLIS = 2_000L

        const val HIGHLIGHTING_LINE_PADDING_START = 10

        const val HIGHLIGHTING_LINE_PADDING_END = HIGHLIGHTING_LINE_PADDING_START + 1
    }

    val lastWrite = atomic(Timestamp(nanoseconds = 0L))

    private suspend fun reloadFromDisk(path: String, snapshot: TextSnapshot) = either {
        val document = documentService.readDocument(path = path).bind()

        val internalContent = snapshot.text

        val externalContent = document.content

        if (internalContent != externalContent) {
            val originalLines = internalContent.split(Regex("(?<=\\n)|(?<=\\r\\n)"))

            val revisedLines = externalContent.split(Regex("(?<=\\n)|(?<=\\r\\n)"))

            val patch = DiffUtils.diff(originalLines, revisedLines)

            val flatOperations = mutableListOf<TextOperation.Data.Single>()

            patch.deltas.sortedByDescending { it.source.position }.forEach { delta ->
                val position = TextPosition(line = delta.source.position, column = 0)

                val newText = delta.target.lines.joinToString(separator = "")

                when (delta.type) {
                    DeltaType.INSERT -> flatOperations.add(
                        TextOperation.Data.Single.Insert(
                            position = position, text = newText
                        )
                    )

                    DeltaType.DELETE -> flatOperations.add(
                        TextOperation.Data.Single.Delete(
                            range = TextRange(
                                start = position,
                                end = TextPosition(line = delta.source.position + delta.source.lines.size, column = 0)
                            )
                        )
                    )

                    DeltaType.CHANGE -> {
                        flatOperations.add(
                            TextOperation.Data.Single.Delete(
                                range = TextRange(
                                    start = position, end = TextPosition(
                                        line = delta.source.position + delta.source.lines.size, column = 0
                                    )
                                )
                            )
                        )

                        flatOperations.add(TextOperation.Data.Single.Insert(position = position, text = newText))
                    }

                    else -> Unit
                }
            }

            if (flatOperations.isNotEmpty()) {
                val data = TextOperation.Data.Batch(operations = flatOperations)

                textService.execute(operation = TextOperation.System(revision = snapshot.revision, data = data)).bind()

                journalService.clear().bind()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeAnalysis() {
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
                                lspService.applyEdit(
                                    path = documentPath, revision = revision, edit = edit
                                ).getOrElse { throwable ->
                                    println(throwable) // todo
                                }
                            }

                            lspService.requestCompletions(
                                path = documentPath, position = position
                            ).getOrElse { throwable ->
                                println(throwable) // todo
                            }

                            lspService.requestReferences(
                                path = documentPath, position = position
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
                                path = documentPath, snapshot = snapshot, range = range
                            ).getOrElse(::println) // todo
                        })

                    else -> null
                }
            }
        }.collect()
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeAutoSave() {
        textService.snapshot.filterNotNull().drop(1).sample(AUTO_SAVE_SAMPLE_MILLIS).collect { snapshot ->
            documentService.saveDocument(path = documentPath, content = snapshot.text).flatMap {
                documentService.getLastModifiedTimestamp(path = documentPath)
            }.onRight { lastModifiedTimestamp ->
                lastWrite.value = lastModifiedTimestamp
            }.getOrElse(::println) // todo
        }
    }

    private suspend fun observeEditing() {
        combine(
            flow = textService.snapshot.filterNotNull(),
            flow2 = textService.edits.onStart { emit(null) },
            transform = { snapshot, edit ->
                editorService.handleEdit(snapshot = snapshot, edit = edit).getOrElse(::println) // todo
            }).collect()
    }

    private suspend fun observeFileSystemChanges() {
        documentService.getParentPath(path = documentPath).flatMap { parentPath ->
            vfsService.observeVisibleFiles(path = parentPath)
        }.map { virtualFiles ->
            virtualFiles.mapNotNull { virtualFiles ->
                virtualFiles.find { virtualFile ->
                    virtualFile.path == documentPath
                }
            }.conflate().filter { virtualFile ->
                virtualFile.lastModified > lastWrite.value
            }.mapNotNull { virtualFile ->
                when (val snapshot = textService.snapshot.value) {
                    null -> Unit

                    else -> reloadFromDisk(path = documentPath, snapshot = snapshot).onRight {
                        lastWrite.value = virtualFile.lastModified
                    }.getOrElse(::println) // todo
                }
            }
        }.getOrElse(::println) // todo
    }

    private suspend fun observeJournaling() {
        textService.edits.filterNotNull().collect { edit ->
            if (edit is TextEdit.User) {
                journalService.push(edit = edit).getOrElse(::println) // todo
            }
        }
    }

    private suspend fun observeSyntax() {
        var fullParseNeeded = true

        combine(
            flow = textService.snapshot,
            flow2 = textService.edits.onStart { emit(null) },
            flow3 = editorService.caret,
            flow4 = editorService.activeLines,
            transform = { snapshot, edit, caret, activeLines ->
                when (snapshot) {
                    null -> {
                        fullParseNeeded = true

                        null
                    }

                    else -> {
                        if (fullParseNeeded) {
                            syntaxService.fullParse(snapshot = snapshot).getOrElse(::println) // todo

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

                        val position = caret.position

                        edit?.data?.let { data ->
                            syntaxService.applyChange(
                                snapshot = snapshot, data = data, range = range, position = position
                            ).getOrElse(::println) // todo
                        }

                        Triple(snapshot, range, position)
                    }
                }
            }).filterNotNull().collectLatest { (snapshot, range, position) ->
            syntaxService.parseFoldingRegions(range = range).getOrElse(::println) // todo

            syntaxService.parseOccurrences(position = position).getOrElse(::println) // todo

            syntaxService.parseTokensPerLine(snapshot = snapshot, range = range).getOrElse(::println) // todo
        }
    }

    override suspend fun Raise<Throwable>.execute(input: Unit): Flow<Editor> {
        val document = documentService.readDocument(path = documentPath).getOrElse { throwable ->
            throw throwable // todo
        }

        val text = document.content

        textService.initialize(initialText = text).flatMap {
            lspService.openDocument(path = documentPath, text = text)
        }.getOrElse { throwable ->
            println(throwable) // todo
        }

        return channelFlow {
            val guideline = document.takeIf(Document::isHaskell)?.let { Guideline.Haskell }

            if (document.isHaskell) {
                launch { observeAnalysis() }

                launch { observeSyntax() }
            }

            launch { observeAutoSave() }

            launch { observeEditing() }

            launch { observeFileSystemChanges() }

            launch { observeJournaling() }

            try {
                val editor = combine(
                    flow = textService.snapshot.filterNotNull(),
                    flow2 = editorService.caret,
                    flow3 = editorService.selection,
                    transform = { snapshot, caret, selection ->
                        Editor(
                            snapshot = snapshot,
                            caret = caret,
                            selection = selection,
                            guideline = guideline,
                            analysis = null,
                            syntax = null
                        )
                    })

                val analysis = when {
                    document.isHaskell -> combine(
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
                        })

                    else -> flowOf(null)
                }

                val syntax = when {
                    document.isHaskell -> syntaxService.syntax.map { syntax ->
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
                    }

                    else -> flowOf(null)
                }

                combine(flow = editor, flow2 = analysis, flow3 = syntax, transform = { editor, analysis, syntax ->
                    editor.copy(analysis = analysis.takeIf { analysis ->
                        analysis?.revision == editor.snapshot.revision
                    }, syntax = syntax.takeIf { syntax ->
                        syntax?.revision == editor.snapshot.revision
                    })
                }).collect(::send)
            } finally {
                val snapshot = textService.snapshot.value

                if (snapshot != null) {
                    documentService.saveDocument(path = documentPath, content = snapshot.text).getOrElse { throwable ->
                        throw throwable // todo
                    }
                }
            }
        }
    }
}