package io.github.numq.haskcore.service.journal

import arrow.core.raise.either
import io.github.numq.haskcore.core.text.TextEdit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class LocalJournalService(
    private val scope: CoroutineScope, private val journalDataSource: JournalDataSource
) : JournalService {
    private companion object {
        const val JOURNAL_LIMIT = 1_000
    }

    override val journal = journalDataSource.journalData.map(JournalData::toJournal).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Journal()
    )

    override suspend fun push(edit: TextEdit.User) = journalDataSource.update { journalData ->
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
    }.map {}

    override suspend fun undo(revision: Long) = either {
        var edit: TextEdit? = null

        journalDataSource.update { data ->
            when {
                data.currentIndex >= 0 -> {
                    val record = data.records[data.currentIndex]

                    when (record.revision) {
                        revision -> {
                            edit = record.toData().invert()

                            data.copy(currentIndex = data.currentIndex - 1)
                        }

                        else -> return@update data.copy(records = emptyList(), currentIndex = -1)
                    }
                }

                else -> data
            }
        }.bind()

        edit
    }

    override suspend fun redo(revision: Long) = either {
        var edit: TextEdit? = null

        journalDataSource.update { data ->
            when {
                data.currentIndex < data.records.size - 1 -> {
                    val nextIndex = data.currentIndex + 1

                    val recordToRedo = data.records[nextIndex]

                    val recordRevision = data.records.lastOrNull()?.revision ?: 0L

                    when {
                        revision > recordRevision -> return@update data.copy(records = emptyList(), currentIndex = -1)

                        else -> {
                            edit = recordToRedo.toData()

                            data.copy(currentIndex = nextIndex)
                        }
                    }
                }

                else -> data
            }
        }.bind()

        edit
    }

    override fun close() {
        scope.cancel()
    }
}