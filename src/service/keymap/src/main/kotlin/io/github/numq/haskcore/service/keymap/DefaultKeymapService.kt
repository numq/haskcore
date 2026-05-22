package io.github.numq.haskcore.service.keymap

import arrow.core.Either
import java.awt.event.KeyEvent

internal class DefaultKeymapService(
    private val actionsByContext: Map<KeymapContext, Set<KeymapAction>>,
) : KeymapService {
    private val actionsByContextAndKey: Map<KeymapContext, Map<Pair<Int, Int>, KeymapAction>> by lazy {
        actionsByContext.mapValues { (_, actions) ->
            buildMap {
                actions.forEach { action ->
                    put(action.keyCode to action.modifiers, action)
                }
            }
        }
    }

    private fun normalizeModifiers(modifiers: Int) =
        modifiers and (KeyEvent.SHIFT_DOWN_MASK or KeyEvent.CTRL_DOWN_MASK or KeyEvent.META_DOWN_MASK or KeyEvent.ALT_DOWN_MASK or KeyEvent.ALT_GRAPH_DOWN_MASK)

    override suspend fun findAction(keyCode: Int, modifiers: Int, context: KeymapContext) = Either.catch {
        val normalizedModifiers = normalizeModifiers(modifiers = modifiers)

        val key = keyCode to normalizedModifiers

        actionsByContextAndKey[context]?.get(key) ?: when (context) {
            KeymapContext.GLOBAL -> null

            else -> actionsByContextAndKey[KeymapContext.GLOBAL]?.get(key)
        }
    }
}