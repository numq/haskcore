package io.github.numq.haskcore.editor.usecase

import io.github.numq.haskcore.editor.EditorRepository
import io.github.numq.haskcore.usecase.UseCase

internal class SaveDocument(
    private val editorRepository: EditorRepository
) : UseCase<Unit, Unit> {
    override suspend fun execute(input: Unit) = editorRepository.saveDocument()
}