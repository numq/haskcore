package io.github.numq.haskcore.service.journal

import arrow.core.raise.either
import io.github.numq.haskcore.common.core.text.TextEdit
import io.github.numq.haskcore.common.core.text.TextRevision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class LocalJournalService(
    private val scope: CoroutineScope, private val journalDataSource: JournalDataSource,
) : JournalService {
    private companion object {
        const val JOURNAL_LIMIT = 1_000
    }

    override val journal = journalDataSource.journalData.map(JournalData::toJournal).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Journal()
    )

    override suspend fun push(edit: TextEdit.User) = journalDataSource.update { journalData ->
        when {
            edit.data.isEffectivelyEmpty() -> journalData

            else -> {
                val record = edit.toJournalRecordData(revision = edit.revision)

                val activeHistory = when {
                    journalData.currentIndex >= 0 && journalData.currentIndex < journalData.records.size - 1 -> journalData.records.subList(
                        0, journalData.currentIndex + 1
                    )

                    journalData.currentIndex == -1 && journalData.records.isNotEmpty() -> emptyList()

                    else -> journalData.records
                }

                val newRecords = (activeHistory + record).takeLast(JOURNAL_LIMIT)

                journalData.copy(records = newRecords, currentIndex = newRecords.size - 1)
            }
        }
    }.map {}

    override suspend fun undo(revision: TextRevision) = either {
        var edit: TextEdit? = null

        journalDataSource.update { journalData ->
            if (journalData.currentIndex >= 0) {
                val record = journalData.records.getOrNull(journalData.currentIndex)

                val originalEdit = record?.toTextEdit()

                when (val inverted = originalEdit?.invert()) {
                    null -> journalData

                    else -> {
                        edit = when (inverted) {
                            is TextEdit.User -> inverted.copy(revision = revision)

                            is TextEdit.System -> inverted.copy(revision = revision)
                        }

                        journalData.copy(currentIndex = journalData.currentIndex - 1)
                    }
                }
            } else {
                journalData
            }
        }.bind()

        edit
    }

    override suspend fun redo(revision: TextRevision) = either {
        var edit: TextEdit? = null

        journalDataSource.update { journalData ->
            if (journalData.currentIndex < journalData.records.size - 1) {
                val nextIndex = journalData.currentIndex + 1

                val recordToRedo = journalData.records.getOrNull(nextIndex)

                when (val originalEdit = recordToRedo?.toTextEdit()) {
                    null -> journalData

                    else -> {
                        edit = when (originalEdit) {
                            is TextEdit.User -> originalEdit.copy(revision = revision)

                            is TextEdit.System -> originalEdit.copy(revision = revision)
                        }

                        journalData.copy(currentIndex = nextIndex)
                    }
                }
            } else {
                journalData
            }
        }.bind()

        edit
    }

    override suspend fun clear() = journalDataSource.update { journalData ->
        journalData.copy(records = emptyList(), currentIndex = -1)
    }.map {}

    override fun close() {
        scope.cancel()
    }
}