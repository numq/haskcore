package io.github.numq.haskcore.service.keymap

import java.awt.event.KeyEvent

internal object Keymap {
    val editor = mapOf(
        KeyStroke(keyCode = KeyEvent.VK_LEFT, modifiers = 0) to "editor.action.moveLeft",
        KeyStroke(keyCode = KeyEvent.VK_RIGHT, modifiers = 0) to "editor.action.moveRight",
        KeyStroke(keyCode = KeyEvent.VK_UP, modifiers = 0) to "editor.action.moveUp",
        KeyStroke(keyCode = KeyEvent.VK_DOWN, modifiers = 0) to "editor.action.moveDown",

        KeyStroke(keyCode = KeyEvent.VK_BACK_SPACE, modifiers = 0) to "editor.action.backspace",
        KeyStroke(keyCode = KeyEvent.VK_ENTER, modifiers = 0) to "editor.action.enter",
        KeyStroke(keyCode = KeyEvent.VK_TAB, modifiers = 0) to "editor.action.tab",

        KeyStroke(keyCode = KeyEvent.VK_A, modifiers = KeyEvent.CTRL_DOWN_MASK) to "editor.action.selectAll",
        KeyStroke(keyCode = KeyEvent.VK_S, modifiers = KeyEvent.CTRL_DOWN_MASK) to "editor.action.save",

        KeyStroke(keyCode = KeyEvent.VK_Z, modifiers = KeyEvent.CTRL_DOWN_MASK) to "editor.action.undo",
        KeyStroke(
            keyCode = KeyEvent.VK_Z, modifiers = KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK
        ) to "editor.action.redo",
        KeyStroke(keyCode = KeyEvent.VK_Y, modifiers = KeyEvent.CTRL_DOWN_MASK) to "editor.action.redo",
    )

    val data = mapOf(KeymapContext.EDITOR to editor)
}