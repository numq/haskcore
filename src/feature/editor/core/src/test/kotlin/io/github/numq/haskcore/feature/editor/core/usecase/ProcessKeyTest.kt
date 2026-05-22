package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.Either
import arrow.core.getOrElse
import io.github.numq.haskcore.common.core.text.*
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.service.clipboard.Clipboard
import io.github.numq.haskcore.service.clipboard.ClipboardService
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.journal.JournalService
import io.github.numq.haskcore.service.keymap.KeymapAction
import io.github.numq.haskcore.service.keymap.KeymapContext
import io.github.numq.haskcore.service.keymap.KeymapService
import io.github.numq.haskcore.service.text.TextService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import java.nio.charset.Charset

internal class ProcessKeyTest {
    private val testPath = "/test/file.txt"

    private lateinit var editorService: EditorService
    private lateinit var clipboardService: ClipboardService
    private lateinit var documentService: DocumentService
    private lateinit var journalService: JournalService
    private lateinit var keymapService: KeymapService
    private lateinit var textService: TextService

    private lateinit var processKey: ProcessKey

    private val caretState = MutableStateFlow(Caret(TextPosition(0, 0)))
    private val selectionState = MutableStateFlow(Selection(TextRange.EMPTY))
    private val snapshotState = MutableStateFlow<TextSnapshot?>(null)
    private val clipboardState = MutableStateFlow(Clipboard())

    private suspend fun processCommand(
        keyCode: Int,
        modifiers: Int,
        utf16CodePoint: Int = 0,
    ) {
        processKey(input = ProcessKey.Input(keyCode, modifiers, utf16CodePoint)).getOrElse { throwable ->
            throw throwable
        }
    }

    private fun mockKeyAction(action: KeymapAction) {
        coEvery {
            keymapService.findAction(any(), any(), KeymapContext.EDITOR)
        } returns Either.Right(action)
    }

    @BeforeEach
    fun setUp() {
        editorService = mockk(relaxed = true)
        clipboardService = mockk(relaxed = true)
        documentService = mockk(relaxed = true)
        journalService = mockk(relaxed = true)
        keymapService = mockk(relaxed = true)
        textService = mockk(relaxed = true)

        every { editorService.caret } returns caretState
        every { editorService.selection } returns selectionState
        every { textService.snapshot } returns snapshotState
        every { clipboardService.clipboard } returns clipboardState

        coEvery { editorService.moveCaretLeft(any()) } returns Either.Right(Unit)
        coEvery { editorService.moveCaretRight(any()) } returns Either.Right(Unit)
        coEvery { editorService.moveCaretUp(any()) } returns Either.Right(Unit)
        coEvery { editorService.moveCaretDown(any()) } returns Either.Right(Unit)
        coEvery { editorService.moveCaret(any(), any()) } returns Either.Right(Unit)
        coEvery { editorService.clearSelection() } returns Either.Right(Unit)
        coEvery { editorService.startSelection(any(), any()) } returns Either.Right(Unit)
        coEvery { editorService.extendSelection(any(), any()) } returns Either.Right(Unit)
        coEvery { editorService.selectAll(any()) } returns Either.Right(Unit)
        coEvery { textService.execute(any()) } returns Either.Right(Unit)
        coEvery { clipboardService.copyToClipboard(any()) } returns Either.Right(Unit)
        coEvery { documentService.saveDocument(any(), any()) } returns Either.Right(Unit)
        coEvery { journalService.undo(any()) } returns Either.Right(null)
        coEvery { journalService.redo(any()) } returns Either.Right(null)

        coEvery { keymapService.findAction(any(), any(), KeymapContext.EDITOR) } returns Either.Right(null)

        processKey = ProcessKey(
            path = testPath,
            editorService = editorService,
            clipboardService = clipboardService,
            documentService = documentService,
            journalService = journalService,
            keymapService = keymapService,
            textService = textService,
        )
    }

    private fun createSnapshot(
        text: String,
        revision: TextRevision = TextRevision.ZERO,
        charset: Charset = Charset.defaultCharset(),
        textLineEnding: TextLineEnding = TextLineEnding.LF,
    ): TextSnapshot {
        val lines = text.split("\n")

        return mockk {
            every { this@mockk.revision } returns revision
            every { this@mockk.charset } returns charset
            every { this@mockk.textLineEnding } returns textLineEnding
            every { this@mockk.lines } returns lines.size
            every { this@mockk.maxLineLength } returns (lines.maxOfOrNull { it.length } ?: 0)
            every { this@mockk.lastPosition } returns TextPosition(
                line = (lines.size - 1).coerceAtLeast(0), column = lines.lastOrNull()?.length ?: 0
            )
            every { this@mockk.text } returns text

            every { isValidPosition(any()) } answers {
                val pos = firstArg<TextPosition>()
                pos.line in 0 until lines.size && pos.column in 0..(lines.getOrNull(pos.line)?.length ?: 0)
            }

            every { getLineText(any()) } answers {
                val line = firstArg<Int>()
                lines.getOrElse(line) { "" }
            }

            every { getLineLength(any()) } answers {
                val line = firstArg<Int>()
                lines.getOrNull(line)?.length ?: 0
            }

            every { getTextInRange(any()) } answers {
                val range = firstArg<TextRange>()
                if (range.isEmpty) return@answers ""

                if (range.start.line == range.end.line) {
                    val line = lines.getOrNull(range.start.line) ?: return@answers ""
                    val start = range.start.column.coerceIn(0, line.length)
                    val end = range.end.column.coerceIn(0, line.length)
                    line.substring(start, end)
                } else {
                    val result = StringBuilder()
                    for (line in range.start.line..range.end.line) {
                        val lineText = lines.getOrNull(line) ?: break
                        when (line) {
                            range.start.line -> {
                                val start = range.start.column.coerceIn(0, lineText.length)
                                result.append(lineText.substring(start))
                            }

                            range.end.line -> {
                                val end = range.end.column.coerceIn(0, lineText.length)
                                result.append("\n")
                                result.append(lineText.substring(0, end))
                            }

                            else -> {
                                result.append("\n")
                                result.append(lineText)
                            }
                        }
                    }
                    result.toString()
                }
            }

            every { getBytePosition(any()) } returns null
            every { getTextPosition(any()) } returns null
        }
    }

    @Test
    fun `move left should call moveCaretLeft and clearSelection`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 5))

        mockKeyAction(KeymapAction.Navigation.Move.Left)

        processCommand(KeyEvent.VK_LEFT, 0)

        coVerify(exactly = 1) { editorService.moveCaretLeft(snapshot) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `move right should call moveCaretRight and clearSelection`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        mockKeyAction(KeymapAction.Navigation.Move.Right)

        processCommand(KeyEvent.VK_RIGHT, 0)

        coVerify(exactly = 1) { editorService.moveCaretRight(snapshot) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `move up should call moveCaretUp and clearSelection`() = runTest {
        val snapshot = createSnapshot("line1\nline2")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(1, 0))

        mockKeyAction(KeymapAction.Navigation.Move.Up)

        processCommand(KeyEvent.VK_UP, 0)

        coVerify(exactly = 1) { editorService.moveCaretUp(snapshot) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `move down should call moveCaretDown and clearSelection`() = runTest {
        val snapshot = createSnapshot("line1\nline2")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        mockKeyAction(KeymapAction.Navigation.Move.Down)

        processCommand(KeyEvent.VK_DOWN, 0)

        coVerify(exactly = 1) { editorService.moveCaretDown(snapshot) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `move left with selection should start selection and extend it`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 5))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Navigation.Move.LeftWithSelection)

        processCommand(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK)

        coVerify(exactly = 1) { editorService.startSelection(snapshot, TextPosition(0, 5)) }
        coVerify(exactly = 1) { editorService.moveCaret(snapshot, TextPosition(0, 4)) }
        coVerify(exactly = 1) { editorService.extendSelection(snapshot, TextPosition(0, 4)) }
    }

    @Test
    fun `move right with selection should extend existing selection`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))
        selectionState.value = Selection(TextRange(TextPosition(0, 0), TextPosition(0, 2)))

        mockKeyAction(KeymapAction.Navigation.Move.RightWithSelection)

        processCommand(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK)

        coVerify(exactly = 0) { editorService.startSelection(any(), any()) }
        coVerify(exactly = 1) { editorService.moveCaret(snapshot, TextPosition(0, 1)) }
        coVerify(exactly = 1) { editorService.extendSelection(snapshot, TextPosition(0, 1)) }
    }

    @Test
    fun `move line start should move to column 0`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 5))

        mockKeyAction(KeymapAction.Navigation.LineMove.Start)

        processCommand(KeyEvent.VK_HOME, 0)

        coVerify(exactly = 1) { editorService.moveCaret(snapshot, TextPosition(0, 0)) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `move line end should move to end of line`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        mockKeyAction(KeymapAction.Navigation.LineMove.End)

        processCommand(KeyEvent.VK_END, 0)

        coVerify(exactly = 1) { editorService.moveCaret(snapshot, TextPosition(0, 11)) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `move word right should jump to next word`() = runTest {
        val snapshot = createSnapshot("hello world test")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        mockKeyAction(KeymapAction.Navigation.WordMove.Right)

        processCommand(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) {
            editorService.moveCaret(snapshot, match { it.line == 0 && it.column > 0 })
        }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }


    @Test
    fun `move word left should jump to previous word`() = runTest {
        val snapshot = createSnapshot("hello world test")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 12))

        mockKeyAction(KeymapAction.Navigation.WordMove.Left)

        processCommand(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) {
            editorService.moveCaret(snapshot, match { it.line == 0 && it.column < 12 })
        }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `backspace should delete previous character`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 2))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.Basic.Backspace)

        processCommand(KeyEvent.VK_BACK_SPACE, 0)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Delete(
                        range = TextRange(TextPosition(0, 1), TextPosition(0, 2))
                    )
                )
            )
        }
    }

    @Test
    fun `backspace at line start should merge with previous line`() = runTest {
        val snapshot = createSnapshot("hello\nworld")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(1, 0))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.Basic.Backspace)

        processCommand(KeyEvent.VK_BACK_SPACE, 0)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Delete(
                        range = TextRange(TextPosition(0, 5), TextPosition(1, 0))
                    )
                )
            )
        }
    }

    @Test
    fun `delete should delete next character`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 2))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.Basic.Delete)

        processCommand(KeyEvent.VK_DELETE, 0)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Delete(
                        range = TextRange(TextPosition(0, 2), TextPosition(0, 3))
                    )
                )
            )
        }
    }

    @Test
    fun `delete at line end should merge with next line`() = runTest {
        val snapshot = createSnapshot("hello\nworld")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 5))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.Basic.Delete)

        processCommand(KeyEvent.VK_DELETE, 0)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Delete(
                        range = TextRange(TextPosition(0, 5), TextPosition(1, 0))
                    )
                )
            )
        }
    }

    @Test
    fun `enter should insert newline`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 2))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.Basic.Enter)

        processCommand(KeyEvent.VK_ENTER, 0)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Insert(
                        position = TextPosition(0, 2), text = "\n"
                    )
                )
            )
        }
    }

    @Test
    fun `enter with selection should replace selection with newline`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 5))
        selectionState.value = Selection(TextRange(TextPosition(0, 0), TextPosition(0, 5)))

        mockKeyAction(KeymapAction.Editing.Basic.Enter)

        processCommand(KeyEvent.VK_ENTER, 0)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Replace(
                        range = TextRange(TextPosition(0, 0), TextPosition(0, 5)), text = "\n"
                    )
                )
            )
        }
    }

    @Test
    fun `tab should insert spaces`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.Basic.Tab)

        processCommand(KeyEvent.VK_TAB, 0)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Insert(
                        position = TextPosition(0, 0), text = "    "
                    )
                )
            )
        }
    }

    @Test
    fun `backspace with selection should delete selected text`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 5))
        selectionState.value = Selection(TextRange(TextPosition(0, 0), TextPosition(0, 5)))

        mockKeyAction(KeymapAction.Editing.Basic.Backspace)

        processCommand(KeyEvent.VK_BACK_SPACE, 0)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Delete(
                        range = TextRange(TextPosition(0, 0), TextPosition(0, 5))
                    )
                )
            )
        }
    }

    @Test
    fun `delete word right should delete to next word boundary`() = runTest {
        val snapshot = createSnapshot("hello world test")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.WordDelete.Right)

        processCommand(KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) {
            textService.execute(match {
                it is TextOperation.User && it.data is TextOperation.Data.Single.Delete && (it.data as TextOperation.Data.Single.Delete).range.start == TextPosition(
                    0, 0
                )
            })
        }
    }

    @Test
    fun `duplicate line should copy line and move caret`() = runTest {
        val snapshot = createSnapshot("hello\nworld")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 2))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.LineOperation.Duplicate)

        processCommand(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Insert(
                        position = TextPosition(1, 0), text = "hello\n"
                    )
                )
            )
        }
        coVerify(exactly = 1) { editorService.moveCaret(any(), TextPosition(1, 2)) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `delete line should remove current line and move caret`() = runTest {
        val snapshot = createSnapshot("hello\nworld\ntest")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(1, 2))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.LineOperation.Delete)

        processCommand(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Delete(
                        range = TextRange(TextPosition(1, 0), TextPosition(2, 0))
                    )
                )
            )
        }
        coVerify(exactly = 1) { editorService.moveCaret(any(), TextPosition(1, 2)) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `delete last line should move caret to previous line`() = runTest {
        val originalSnapshot = createSnapshot("hello\nworld")
        val updatedSnapshot = createSnapshot("hello")

        every { textService.snapshot.value } returnsMany listOf(originalSnapshot, updatedSnapshot)

        snapshotState.value = originalSnapshot
        caretState.value = Caret(TextPosition(1, 2))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.LineOperation.Delete)

        processCommand(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { editorService.moveCaret(updatedSnapshot, TextPosition(0, 2)) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `copy should copy selected text to clipboard`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 5))
        selectionState.value = Selection(TextRange(TextPosition(0, 0), TextPosition(0, 5)))

        mockKeyAction(KeymapAction.Clipboard.Copy)

        processCommand(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { clipboardService.copyToClipboard("hello") }
        coVerify(exactly = 0) { textService.execute(any()) }
    }

    @Test
    fun `copy without selection should copy current line`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Clipboard.Copy)

        processCommand(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { clipboardService.copyToClipboard("hello world") }
    }

    @Test
    fun `paste should insert clipboard text`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 6))
        selectionState.value = Selection(TextRange.EMPTY)
        clipboardState.value = Clipboard(text = "beautiful ")

        mockKeyAction(KeymapAction.Clipboard.Paste)

        processCommand(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Insert(
                        position = TextPosition(0, 6), text = "beautiful "
                    )
                )
            )
        }
    }

    @Test
    fun `paste with selection should replace selection`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 5))
        selectionState.value = Selection(TextRange(TextPosition(0, 0), TextPosition(0, 5)))
        clipboardState.value = Clipboard(text = "hi")

        mockKeyAction(KeymapAction.Clipboard.Paste)

        processCommand(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Replace(
                        range = TextRange(TextPosition(0, 0), TextPosition(0, 5)), text = "hi"
                    )
                )
            )
        }
    }

    @Test
    fun `cut should copy and delete selected text`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 5))
        selectionState.value = Selection(TextRange(TextPosition(0, 0), TextPosition(0, 5)))

        mockKeyAction(KeymapAction.Clipboard.Cut)

        processCommand(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { clipboardService.copyToClipboard("hello") }
        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Delete(
                        range = TextRange(TextPosition(0, 0), TextPosition(0, 5))
                    )
                )
            )
        }
    }

    @Test
    fun `undo should call journal service and execute operation`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        val edit = mockk<TextEdit>(relaxed = true)
        coEvery { journalService.undo(any()) } returns Either.Right(edit)

        mockKeyAction(KeymapAction.History.Undo)

        processCommand(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { journalService.undo(TextRevision.ZERO) }
        coVerify(exactly = 1) { textService.execute(any()) }
    }

    @Test
    fun `undo should do nothing if journal returns null`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        coEvery { journalService.undo(any()) } returns Either.Right(null)

        mockKeyAction(KeymapAction.History.Undo)

        processCommand(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { journalService.undo(TextRevision.ZERO) }
        coVerify(exactly = 0) { textService.execute(any()) }
    }

    @Test
    fun `redo should call journal service and execute operation`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        val edit = mockk<TextEdit>(relaxed = true)
        coEvery { journalService.redo(any()) } returns Either.Right(edit)

        mockKeyAction(KeymapAction.History.Redo)

        processCommand(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK)

        coVerify(exactly = 1) { journalService.redo(TextRevision.ZERO) }
        coVerify(exactly = 1) { textService.execute(any()) }
    }

    @Test
    fun `redo with RedoAlt should call journal service and execute operation`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        val edit = mockk<TextEdit>(relaxed = true)
        coEvery { journalService.redo(any()) } returns Either.Right(edit)

        mockKeyAction(KeymapAction.History.RedoAlt)

        processCommand(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { journalService.redo(TextRevision.ZERO) }
        coVerify(exactly = 1) { textService.execute(any()) }
    }

    @Test
    fun `redo should do nothing if journal returns null`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        coEvery { journalService.redo(any()) } returns Either.Right(null)

        mockKeyAction(KeymapAction.History.Redo)

        processCommand(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK)

        coVerify(exactly = 1) { journalService.redo(TextRevision.ZERO) }
        coVerify(exactly = 0) { textService.execute(any()) }
    }

    @Test
    fun `select all should call editor service`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        mockKeyAction(KeymapAction.File.SelectAll)

        processCommand(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { editorService.selectAll(snapshot) }
    }

    @Test
    fun `save should call document service with correct path and content`() = runTest {
        val snapshot = createSnapshot("hello world\nsecond line")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        mockKeyAction(KeymapAction.File.Save)

        processCommand(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { documentService.saveDocument(testPath, "hello world\nsecond line") }
    }

    @Test
    fun `printable character should insert text`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))
        selectionState.value = Selection(TextRange.EMPTY)

        processCommand(KeyEvent.VK_A, 0, 'A'.code)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Insert(
                        position = TextPosition(0, 0), text = "A"
                    )
                )
            )
        }
    }

    @Test
    fun `printable character with selection should replace selection`() = runTest {
        val snapshot = createSnapshot("hello world")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 5))
        selectionState.value = Selection(TextRange(TextPosition(0, 0), TextPosition(0, 5)))

        processCommand(KeyEvent.VK_A, 0, 'A'.code)

        coVerify(exactly = 1) {
            textService.execute(
                TextOperation.User(
                    revision = TextRevision.ZERO, data = TextOperation.Data.Single.Replace(
                        range = TextRange(TextPosition(0, 0), TextPosition(0, 5)), text = "A"
                    )
                )
            )
        }
    }

    @Test
    fun `non-printable character should be ignored`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        processCommand(KeyEvent.VK_ESCAPE, 0, 27)

        coVerify(exactly = 0) { textService.execute(any()) }
        coVerify(exactly = 0) { editorService.moveCaret(any(), any()) }
    }

    @Test
    fun `should not process if snapshot is null`() = runTest {
        snapshotState.value = null

        mockKeyAction(KeymapAction.Navigation.Move.Left)

        processCommand(KeyEvent.VK_LEFT, 0)

        coVerify(exactly = 0) { editorService.moveCaretLeft(any()) }
    }

    @Test
    fun `should not process if caret position is invalid`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(10, 0))

        mockKeyAction(KeymapAction.Navigation.Move.Left)

        processCommand(KeyEvent.VK_LEFT, 0)

        coVerify(exactly = 0) { editorService.moveCaretLeft(any()) }
    }

    @Test
    fun `backspace at start of document should do nothing`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition.ZERO)
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.Basic.Backspace)

        processCommand(KeyEvent.VK_BACK_SPACE, 0)

        coVerify(exactly = 0) { textService.execute(any()) }
    }

    @Test
    fun `delete at end of document should do nothing`() = runTest {
        val snapshot = createSnapshot("hello")
        val lastLine = snapshot.lines - 1
        val lastCol = snapshot.getLineLength(lastLine)
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(lastLine, lastCol))
        selectionState.value = Selection(TextRange.EMPTY)

        mockKeyAction(KeymapAction.Editing.Basic.Delete)

        processCommand(KeyEvent.VK_DELETE, 0)

        coVerify(exactly = 0) { textService.execute(any()) }
    }

    @Test
    fun `paste with empty clipboard should do nothing`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))
        clipboardState.value = Clipboard(text = null)

        mockKeyAction(KeymapAction.Clipboard.Paste)

        processCommand(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 0) { textService.execute(any()) }
    }

    @Test
    fun `paste with blank clipboard should do nothing`() = runTest {
        val snapshot = createSnapshot("hello")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))
        clipboardState.value = Clipboard(text = "")

        mockKeyAction(KeymapAction.Clipboard.Paste)

        processCommand(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 0) { textService.execute(any()) }
    }

    @Test
    fun `document move start should move to zero position`() = runTest {
        val snapshot = createSnapshot("hello\nworld\ntest")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(2, 2))

        mockKeyAction(KeymapAction.Navigation.DocumentMove.Start)

        processCommand(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { editorService.moveCaret(snapshot, TextPosition.ZERO) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }

    @Test
    fun `document move end should move to last position`() = runTest {
        val snapshot = createSnapshot("hello\nworld")
        snapshotState.value = snapshot
        caretState.value = Caret(TextPosition(0, 0))

        mockKeyAction(KeymapAction.Navigation.DocumentMove.End)

        processCommand(KeyEvent.VK_END, KeyEvent.CTRL_DOWN_MASK)

        coVerify(exactly = 1) { editorService.moveCaret(snapshot, TextPosition(1, 5)) }
        coVerify(exactly = 1) { editorService.clearSelection() }
    }
}