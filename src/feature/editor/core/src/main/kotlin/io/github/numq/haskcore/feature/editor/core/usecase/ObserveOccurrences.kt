package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.highlighting.HighlightingToken
import io.github.numq.haskcore.feature.editor.core.highlighting.HighlightingType
import io.github.numq.haskcore.feature.editor.core.occurrences.Occurrences
import io.github.numq.haskcore.service.text.TextService
import io.github.numq.haskcore.service.text.occurrence.Occurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveOccurrences(
    private val editorService: EditorService, private val textService: TextService
) : UseCase<Unit, Flow<Occurrences>> {
    private fun Occurrence.toHighlightingType() = when (this) {
        is Occurrence.Definition -> HighlightingType.LOCAL_DEFINITION

        is Occurrence.Reference, is Occurrence.Read, is Occurrence.Write -> HighlightingType.LOCAL_REFERENCE
    }

    override suspend fun Raise<Throwable>.execute(input: Unit) = textService.snapshot.map { snapshot ->
        when (snapshot) {
            null -> Occurrences()

            else -> {
                val caret = editorService.caret.value

                val occurrences = textService.getLocalOccurrences(position = caret.position).bind()

                val tokens = occurrences.map { occurrence ->
                    val text = snapshot.getTextInRange(range = occurrence.range)

                    HighlightingToken.Atom(
                        range = occurrence.range,
                        type = occurrence.toHighlightingType(),
                        text = text
                    )
                }

                Occurrences(tokens = tokens)
            }
        }
    }
}