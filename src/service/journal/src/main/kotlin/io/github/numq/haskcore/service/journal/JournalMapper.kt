package io.github.numq.haskcore.service.journal

import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition

internal fun TextEdit.toJournalRecordData(revision: Long) = data.toJournalRecordData(revision = revision)

internal fun TextEdit.Data.toJournalRecordData(revision: Long): JournalRecordData = when (this) {
    is TextEdit.Data.Single.Insert -> JournalRecordData.Insert(
        revision = revision,
        startByte = startByte,
        newEndByte = newEndByte,
        startLine = startPosition.line,
        startColumn = startPosition.column,
        newEndLine = newEndPosition.line,
        newEndColumn = newEndPosition.column,
        text = insertedText,
        timestampNanos = timestamp.nanoseconds
    )

    is TextEdit.Data.Single.Replace -> JournalRecordData.Replace(
        revision = revision,
        startByte = startByte,
        oldEndByte = oldEndByte,
        newEndByte = newEndByte,
        startLine = startPosition.line,
        startColumn = startPosition.column,
        oldEndLine = oldEndPosition.line,
        oldEndColumn = oldEndPosition.column,
        newEndLine = newEndPosition.line,
        newEndColumn = newEndPosition.column,
        oldText = oldText,
        newText = newText,
        timestampNanos = timestamp.nanoseconds
    )

    is TextEdit.Data.Single.Delete -> JournalRecordData.Delete(
        revision = revision,
        startByte = startByte,
        oldEndByte = oldEndByte,
        startLine = startPosition.line,
        startColumn = startPosition.column,
        oldEndLine = oldEndPosition.line,
        oldEndColumn = oldEndPosition.column,
        text = deletedText,
        timestampNanos = timestamp.nanoseconds
    )

    is TextEdit.Data.Batch -> JournalRecordData.Batch(
        revision = revision,
        timestampNanos = timestamp.nanoseconds,
        records = singles.map { single -> single.toJournalRecordData(revision = revision) })
}

internal fun JournalRecordData.toData(): TextEdit {
    val data = when (this) {
        is JournalRecordData.Insert -> TextEdit.Data.Single.Insert(
            startPosition = TextPosition(line = startLine, column = startColumn),
            newEndPosition = TextPosition(line = newEndLine, column = newEndColumn),
            insertedText = text,
            startByte = startByte,
            newEndByte = newEndByte
        )

        is JournalRecordData.Replace -> TextEdit.Data.Single.Replace(
            startPosition = TextPosition(line = startLine, column = startColumn),
            oldEndPosition = TextPosition(line = oldEndLine, column = oldEndColumn),
            newEndPosition = TextPosition(line = newEndLine, column = newEndColumn),
            oldText = oldText,
            newText = newText,
            startByte = startByte,
            oldEndByte = oldEndByte,
            newEndByte = newEndByte
        )

        is JournalRecordData.Delete -> TextEdit.Data.Single.Delete(
            startPosition = TextPosition(line = startLine, column = startColumn),
            oldEndPosition = TextPosition(line = oldEndLine, column = oldEndColumn),
            deletedText = text,
            startByte = startByte,
            oldEndByte = oldEndByte
        )

        is JournalRecordData.Batch -> TextEdit.Data.Batch(singles = records.mapNotNull { journalRecordData ->
            journalRecordData.toData() as? TextEdit.Data.Single
        })
    }

    return TextEdit.User(data = data, revision = revision)
}

internal fun JournalData.toJournal() = Journal(
    edits = records.map(JournalRecordData::toData), currentIndex = currentIndex
)