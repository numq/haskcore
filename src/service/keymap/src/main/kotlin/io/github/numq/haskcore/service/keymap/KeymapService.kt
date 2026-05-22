package io.github.numq.haskcore.service.keymap

import arrow.core.Either

interface KeymapService {
    suspend fun findAction(keyCode: Int, modifiers: Int, context: KeymapContext): Either<Throwable, KeymapAction?>
}