package io.github.numq.haskcore.service.keymap

import arrow.core.Either

interface KeymapService {
    fun getActionId(keyStroke: KeyStroke, context: KeymapContext): Either<Throwable, String?>

    suspend fun getKeyStrokes(actionId: String): Either<Throwable, List<KeyStroke>>
}