package io.github.numq.haskcore.service.journal

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition
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
    fun setup() {
        every { journalDataSource.journalData } returns journalDataFlow
    }

    @Test
    fun `push should add record and update index`() = runTest(UnconfinedTestDispatcher()) {
        val service = LocalJournalService(backgroundScope, journalDataSource)

        val edit = TextEdit.User(
            data = TextEdit.Data.Single.Insert(
                startPosition = TextPosition(0, 0),
                newEndPosition = TextPosition(0, 5),
                insertedText = "hello",
                startByte = 0,
                newEndByte = 5
            ), revision = 1L
        )

        coEvery { journalDataSource.update(any()) } answers {
            val transform = firstArg<(JournalData) -> JournalData>()
            val newData = transform(journalDataFlow.value)
            journalDataFlow.value = newData
            Either.Right(newData)
        }

        service.push(edit)

        val currentJournal = service.journal.value
        assertEquals(0, currentJournal.currentIndex)
        assertEquals(1, currentJournal.edits.size)
        assertEquals(1L, currentJournal.edits.first().revision)
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

        coEvery { journalDataSource.update(any()) } answers {
            val transform = firstArg<(JournalData) -> JournalData>()
            val newData = transform(journalDataFlow.value)
            journalDataFlow.value = newData
            Either.Right(newData)
        }

        val result = service.undo(1L)

        assertTrue(result.isRight())
        assertEquals(-1, service.journal.value.currentIndex)

        result.onRight { edit ->
            assertTrue(edit?.data is TextEdit.Data.Single.Delete)
        }
    }
}