package io.github.numq.haskcore.service.keymap

import java.awt.event.KeyEvent

sealed interface KeymapAction {
    val keyCode: Int

    val modifiers: Int

    sealed interface Navigation : KeymapAction {
        sealed interface Move : Navigation {
            data object Left : Move {
                override val keyCode = KeyEvent.VK_LEFT

                override val modifiers = 0
            }

            data object Right : Move {
                override val keyCode = KeyEvent.VK_RIGHT

                override val modifiers = 0
            }

            data object Up : Move {
                override val keyCode = KeyEvent.VK_UP

                override val modifiers = 0
            }

            data object Down : Move {
                override val keyCode = KeyEvent.VK_DOWN

                override val modifiers = 0
            }

            data object LeftWithSelection : Move {
                override val keyCode = KeyEvent.VK_LEFT

                override val modifiers = KeyEvent.SHIFT_DOWN_MASK
            }

            data object RightWithSelection : Move {
                override val keyCode = KeyEvent.VK_RIGHT

                override val modifiers = KeyEvent.SHIFT_DOWN_MASK
            }

            data object UpWithSelection : Move {
                override val keyCode = KeyEvent.VK_UP

                override val modifiers = KeyEvent.SHIFT_DOWN_MASK
            }

            data object DownWithSelection : Move {
                override val keyCode = KeyEvent.VK_DOWN

                override val modifiers = KeyEvent.SHIFT_DOWN_MASK
            }
        }

        sealed interface WordMove : Navigation {
            data object Left : WordMove {
                override val keyCode = KeyEvent.VK_LEFT

                override val modifiers = KeyEvent.CTRL_DOWN_MASK
            }

            data object Right : WordMove {
                override val keyCode = KeyEvent.VK_RIGHT

                override val modifiers = KeyEvent.CTRL_DOWN_MASK
            }

            data object LeftWithSelection : WordMove {
                override val keyCode = KeyEvent.VK_LEFT

                override val modifiers = KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK
            }

            data object RightWithSelection : WordMove {
                override val keyCode = KeyEvent.VK_RIGHT

                override val modifiers = KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK
            }
        }

        sealed interface LineMove : Navigation {
            data object Start : LineMove {
                override val keyCode = KeyEvent.VK_HOME

                override val modifiers = 0
            }

            data object End : LineMove {
                override val keyCode = KeyEvent.VK_END

                override val modifiers = 0
            }

            data object StartWithSelection : LineMove {
                override val keyCode = KeyEvent.VK_HOME

                override val modifiers = KeyEvent.SHIFT_DOWN_MASK
            }

            data object EndWithSelection : LineMove {
                override val keyCode = KeyEvent.VK_END

                override val modifiers = KeyEvent.SHIFT_DOWN_MASK
            }
        }

        sealed interface DocumentMove : Navigation {
            data object Start : DocumentMove {
                override val keyCode = KeyEvent.VK_HOME

                override val modifiers = KeyEvent.CTRL_DOWN_MASK
            }

            data object End : DocumentMove {
                override val keyCode = KeyEvent.VK_END

                override val modifiers = KeyEvent.CTRL_DOWN_MASK
            }

            data object StartWithSelection : DocumentMove {
                override val keyCode = KeyEvent.VK_HOME

                override val modifiers = KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK
            }

            data object EndWithSelection : DocumentMove {
                override val keyCode = KeyEvent.VK_END

                override val modifiers = KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK
            }
        }
    }

    sealed interface Editing : KeymapAction {
        sealed interface Basic : Editing {
            data object Backspace : Basic {
                override val keyCode = KeyEvent.VK_BACK_SPACE

                override val modifiers = 0
            }

            data object Delete : Basic {
                override val keyCode = KeyEvent.VK_DELETE

                override val modifiers = 0
            }

            data object Enter : Basic {
                override val keyCode = KeyEvent.VK_ENTER

                override val modifiers = 0
            }

            data object Tab : Basic {
                override val keyCode = KeyEvent.VK_TAB

                override val modifiers = 0
            }
        }

        sealed interface WordDelete : Editing {
            data object Left : WordDelete {
                override val keyCode = KeyEvent.VK_BACK_SPACE

                override val modifiers = KeyEvent.CTRL_DOWN_MASK
            }

            data object Right : WordDelete {
                override val keyCode = KeyEvent.VK_DELETE

                override val modifiers = KeyEvent.CTRL_DOWN_MASK
            }
        }

        sealed interface LineOperation : Editing {
            data object Duplicate : LineOperation {
                override val keyCode = KeyEvent.VK_D

                override val modifiers = KeyEvent.CTRL_DOWN_MASK
            }

            data object Delete : LineOperation {
                override val keyCode = KeyEvent.VK_L

                override val modifiers = KeyEvent.CTRL_DOWN_MASK
            }
        }
    }

    sealed interface Clipboard : KeymapAction {
        data object Cut : Clipboard {
            override val keyCode = KeyEvent.VK_X

            override val modifiers = KeyEvent.CTRL_DOWN_MASK
        }

        data object Copy : Clipboard {
            override val keyCode = KeyEvent.VK_C

            override val modifiers = KeyEvent.CTRL_DOWN_MASK
        }

        data object Paste : Clipboard {
            override val keyCode = KeyEvent.VK_V

            override val modifiers = KeyEvent.CTRL_DOWN_MASK
        }
    }

    sealed interface History : KeymapAction {
        data object Undo : History {
            override val keyCode = KeyEvent.VK_Z

            override val modifiers = KeyEvent.CTRL_DOWN_MASK
        }

        data object Redo : History {
            override val keyCode = KeyEvent.VK_Z

            override val modifiers = KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK
        }

        data object RedoAlt : History {
            override val keyCode = KeyEvent.VK_Y

            override val modifiers = KeyEvent.CTRL_DOWN_MASK
        }
    }

    sealed interface File : KeymapAction {
        data object SelectAll : File {
            override val keyCode = KeyEvent.VK_A

            override val modifiers = KeyEvent.CTRL_DOWN_MASK
        }

        data object Save : File {
            override val keyCode = KeyEvent.VK_S

            override val modifiers = KeyEvent.CTRL_DOWN_MASK
        }
    }
}