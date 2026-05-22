package io.github.numq.haskcore.service.journal

import arrow.core.Either
import io.github.numq.haskcore.common.core.text.TextEdit
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRevision
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalJournalServiceTest {
    private val journalDataSource = mockk<JournalDataSource>()
    private val journalDataFlow = MutableStateFlow(JournalData())

    @BeforeEach
    fun setUp() {
        every { journalDataSource.journalData } returns journalDataFlow

        coEvery { journalDataSource.update(any()) } answers {
            val transform = firstArg<(JournalData) -> JournalData>()
            val newData = transform(journalDataFlow.value)
            journalDataFlow.value = newData
            Either.Right(newData)
        }
    }

    @Test
    fun `push should add record and update index`() = runTest(UnconfinedTestDispatcher()) {
        val service = LocalJournalService(backgroundScope, journalDataSource)

        val targetRevision = TextRevision(value = 0)
        val edit = TextEdit.User(
            data = TextEdit.Data.Single.Insert(
                startPosition = TextPosition(0, 0),
                newEndPosition = TextPosition(0, 5),
                insertedText = "hello",
                startByte = 0,
                newEndByte = 5
            ), revision = targetRevision
        )

        service.push(edit)

        assertEquals(0, journalDataFlow.value.currentIndex)

        val currentJournal = service.journal.value
        assertEquals(1, currentJournal.edits.size)
        assertEquals(targetRevision.value, currentJournal.edits.first().revision.value)
    }

    @Test
    fun `undo should decrease index and return inverted edit`() = runTest(UnconfinedTestDispatcher()) {
        val initialRecord = JournalRecordData.Insert(
            revision = 1L,
            startByte = 0,
            newEndByte = 5,
            startLine = 0,
            startColumn = 0,
            newEndLine = 0,
            newEndColumn = 5,
            timestampNanos = 1000L,
            text = "hello"
        )
        journalDataFlow.value = JournalData(records = listOf(initialRecord), currentIndex = 0)

        val service = LocalJournalService(backgroundScope, journalDataSource)

        val currentSnapshotRevision = TextRevision(value = 2)
        val result = service.undo(currentSnapshotRevision)

        assertTrue(result.isRight())

        assertEquals(-1, journalDataFlow.value.currentIndex)

        result.onRight { edit ->
            assertTrue(edit?.data is TextEdit.Data.Single.Delete)
            assertEquals(currentSnapshotRevision.value, edit?.revision?.value)
        }
    }
}