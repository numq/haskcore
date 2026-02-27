package io.github.numq.haskcore.service.keymap

import arrow.core.Either

internal class DefaultKeymapService(
    private val keymapData: Map<KeymapContext, Map<KeyStroke, String>>
) : KeymapService {
    override fun getActionId(
        keyStroke: KeyStroke, context: KeymapContext
    ) = Either.catch {
        val localAction = keymapData[context]?.get(keyStroke)

        when (localAction) {
            null if context != KeymapContext.GLOBAL -> keymapData[KeymapContext.GLOBAL]?.get(keyStroke)

            else -> localAction
        }
    }

    override suspend fun getKeyStrokes(actionId: String) = Either.catch {
        keymapData.values.flatMap { keyStrokes ->
            keyStrokes.entries
        }.filter { keyStroke ->
            keyStroke.value == actionId
        }.map(Map.Entry<KeyStroke, String>::key).distinct()
    }
}