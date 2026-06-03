package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import io.github.numq.haskcore.common.core.text.TextEdit
import io.github.numq.haskcore.common.core.text.TextOperation
import io.github.numq.haskcore.common.core.text.TextSnapshot
import io.github.numq.haskcore.common.core.timestamp.Timestamp
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.Editor
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.journal.JournalService
import io.github.numq.haskcore.service.logger.LoggerService
import io.github.numq.haskcore.service.text.TextService
import io.github.numq.haskcore.service.vfs.VfsService
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ObserveEditor(
    private val editorService: EditorService,
    private val documentService: DocumentService,
    private val journalService: JournalService,
    private val loggerService: LoggerService, // todo
    private val textService: TextService,
    private val vfsService: VfsService,
) : UseCase.Exchange<ObserveEditor.Input, Flow<Editor>> {
    data class Input(val path: String)

    private companion object {
        const val AUTO_SAVE_SAMPLE_MILLIS = 2_000L
    }

    val lastWrite = atomic(Timestamp(nanoseconds = 0L))

    private suspend fun reloadFromDisk(path: String, snapshot: TextSnapshot) = either {
        val document = documentService.readDocument(path = path).bind()

        val internalContent = snapshot.text

        val externalContent = document.content

        if (internalContent != externalContent) {
            val batch = textService.computeDifference(original = internalContent, revised = externalContent).bind()

            textService.execute(
                operation = TextOperation.System(revision = snapshot.revision, data = batch)
            ).bind()
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeAutoSave(path: String) {
        textService.snapshot.filterNotNull().drop(1).sample(AUTO_SAVE_SAMPLE_MILLIS).collect { snapshot ->
            documentService.saveDocument(path = path, content = snapshot.text, encoding = snapshot.encoding).flatMap {
                editorService.getLastModifiedTimestamp(path = path)
            }.onRight { lastModifiedTimestamp ->
                lastWrite.value = lastModifiedTimestamp
            }.getOrElse(::println) // todo
        }
    }

    private suspend fun observeEditing() {
        textService.edits.collect { edit ->
            val snapshot = textService.snapshot.value ?: return@collect

            editorService.handleEdit(snapshot = snapshot, edit = edit).getOrElse(::println) // todo
        }
    }

    private suspend fun observeFileSystemChanges(path: String) {
        editorService.getParentPath(path = path).flatMap { parentPath ->
            vfsService.observeVisibleFiles(path = parentPath)
        }.map { virtualFiles ->
            virtualFiles.mapNotNull { virtualFiles ->
                virtualFiles.find { virtualFile ->
                    virtualFile.path == path
                }
            }.conflate().filter { virtualFile ->
                virtualFile.lastModifiedTimestamp > lastWrite.value
            }.mapNotNull { virtualFile ->
                when (val snapshot = textService.snapshot.value) {
                    null -> Unit

                    else -> reloadFromDisk(path = path, snapshot = snapshot).onRight {
                        lastWrite.value = virtualFile.lastModifiedTimestamp
                    }.getOrElse(::println) // todo
                }
            }
        }.getOrElse(::println) // todo
    }

    private suspend fun observeJournaling() {
        textService.edits.filterNotNull().collect { edit ->
            if (edit is TextEdit.User && !edit.data.isEffectivelyEmpty()) {
                journalService.push(edit = edit).getOrElse(::println) // todo
            }
        }
    }

    override suspend fun Raise<Throwable>.exchange(input: Input) = with(input) {
        val document = documentService.readDocument(path = path).getOrElse { throwable ->
            throw throwable // todo
        }

        val language = document.metadata.language

        val text = document.content

        textService.initialize(initialText = text).getOrElse(::println) // todo

        channelFlow {
            launch { observeAutoSave(path = path) }

            launch { observeEditing() }

            launch { observeFileSystemChanges(path = path) }

            launch { observeJournaling() }

            try {
                combine(
                    flow = textService.snapshot.filterNotNull(),
                    flow2 = editorService.caret,
                    flow3 = editorService.selection,
                    transform = { snapshot, caret, selection ->
                        Editor(language = language, snapshot = snapshot, caret = caret, selection = selection)
                    }).collect(::send)
            } finally {
                val snapshot = textService.snapshot.value

                if (snapshot != null) {
                    documentService.saveDocument(
                        path = path, content = snapshot.text, encoding = snapshot.encoding
                    ).getOrElse { throwable ->
                        throw throwable // todo
                    }
                }
            }
        }
    }
}