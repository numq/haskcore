package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType
import io.github.numq.haskcore.core.text.*
import io.github.numq.haskcore.core.timestamp.Timestamp
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.journal.JournalService
import io.github.numq.haskcore.service.text.TextService
import io.github.numq.haskcore.service.vfs.VfsService
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.getLastModifiedTime

class ObserveTextSnapshot(
    private val path: String,
    private val editorService: EditorService,
    private val documentService: DocumentService,
    private val journalService: JournalService,
    private val textService: TextService,
    private val vfsService: VfsService,
) : UseCase<Unit, Flow<TextSnapshot>> {
    private companion object {
        const val AUTO_SAVE_SAMPLE_MILLIS = 2_000L
    }

    private val lastWrite = atomic(Timestamp(nanoseconds = 0L))

    private suspend fun reloadFromDisk(path: String, content: String) = either {
        val document = documentService.readDocument(path = path).bind()

        val externalContent = document.content

        if (externalContent != content) {
            val originalLines = content.split(Regex("(?<=\\n)|(?<=\\r\\n)"))

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

                textService.execute(operation = TextOperation.System(data = data)).bind()

                journalService.clear().bind()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun CoroutineScope.launchSyncProcesses() {
        textService.edits.filterNotNull().onEach { edit ->
            if (edit is TextEdit.User) {
                journalService.push(edit = edit).getOrElse { throwable ->
                    println(throwable) // todo
                }
            }

            textService.snapshot.value?.let { snapshot ->
                editorService.handleEdit(snapshot = snapshot, edit = edit).getOrElse { throwable ->
                    println(throwable) // todo
                }
            }
        }.launchIn(scope = this)

        textService.snapshot.filterNotNull().drop(1).sample(AUTO_SAVE_SAMPLE_MILLIS).onEach { snapshot ->
            documentService.saveDocument(path, snapshot.text).fold(ifLeft = { throwable ->
                println(throwable) // todo
            }, ifRight = {
                val lastModifiedNanos = Path.of(path).getLastModifiedTime().to(TimeUnit.NANOSECONDS)

                lastWrite.value = Timestamp(nanoseconds = lastModifiedNanos)
            })
        }.launchIn(scope = this)

        vfsService.observeDirectory(path = Path.of(path).parent.toString()).fold(ifLeft = { throwable ->
            println(throwable) // todo
        }, ifRight = { virtualFiles ->
            virtualFiles.mapNotNull { virtualFiles ->
                virtualFiles.find { virtualFile ->
                    virtualFile.path == path
                }
            }.conflate().filter { virtualFile ->
                virtualFile.lastModified > lastWrite.value
            }.onEach { virtualFile ->
                when (val content = textService.snapshot.value?.text) {
                    null -> Unit

                    else -> reloadFromDisk(path = path, content = content).fold(ifLeft = { throwable ->
                        println(throwable) // todo
                    }, ifRight = {
                        lastWrite.value = virtualFile.lastModified
                    })
                }
            }.launchIn(scope = this)
        })
    }

    override suspend fun Raise<Throwable>.execute(input: Unit): Flow<TextSnapshot> {
        if (textService.snapshot.value == null) {
            val document = documentService.readDocument(path = path).bind()

            textService.initialize(initialText = document.content).bind()
        }

        return channelFlow {
            launchSyncProcesses()

            try {
                textService.snapshot.filterNotNull().distinctUntilChanged().collect(::send)
            } finally {
                val snapshot = textService.snapshot.value

                if (snapshot != null) {
                    documentService.saveDocument(path = path, content = snapshot.text).getOrElse { throwable ->
                        throw throwable // todo
                    }
                }
            }
        }
    }
}