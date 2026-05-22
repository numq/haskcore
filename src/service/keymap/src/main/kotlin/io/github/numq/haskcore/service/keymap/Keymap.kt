package io.github.numq.haskcore.service.keymap

internal object Keymap {
    val editorActions = setOf(
        KeymapAction.Navigation.Move.Left,
        KeymapAction.Navigation.Move.Right,
        KeymapAction.Navigation.Move.Up,
        KeymapAction.Navigation.Move.Down,
        KeymapAction.Navigation.Move.LeftWithSelection,
        KeymapAction.Navigation.Move.RightWithSelection,
        KeymapAction.Navigation.Move.UpWithSelection,
        KeymapAction.Navigation.Move.DownWithSelection,

        KeymapAction.Navigation.WordMove.Left,
        KeymapAction.Navigation.WordMove.Right,
        KeymapAction.Navigation.WordMove.LeftWithSelection,
        KeymapAction.Navigation.WordMove.RightWithSelection,

        KeymapAction.Navigation.LineMove.Start,
        KeymapAction.Navigation.LineMove.End,
        KeymapAction.Navigation.LineMove.StartWithSelection,
        KeymapAction.Navigation.LineMove.EndWithSelection,

        KeymapAction.Navigation.DocumentMove.Start,
        KeymapAction.Navigation.DocumentMove.End,
        KeymapAction.Navigation.DocumentMove.StartWithSelection,
        KeymapAction.Navigation.DocumentMove.EndWithSelection,

        KeymapAction.Editing.Basic.Backspace,
        KeymapAction.Editing.Basic.Delete,
        KeymapAction.Editing.Basic.Enter,
        KeymapAction.Editing.Basic.Tab,

        KeymapAction.Editing.WordDelete.Left,
        KeymapAction.Editing.WordDelete.Right,

        KeymapAction.Editing.LineOperation.Duplicate,
        KeymapAction.Editing.LineOperation.Delete,

        KeymapAction.Clipboard.Cut,
        KeymapAction.Clipboard.Copy,
        KeymapAction.Clipboard.Paste,

        KeymapAction.History.Undo,
        KeymapAction.History.Redo,

        KeymapAction.File.SelectAll,
        KeymapAction.File.Save,
    )

    val actionsByContext = mapOf(
        KeymapContext.GLOBAL to emptySet(),
        KeymapContext.EDITOR to editorActions,
        KeymapContext.FILE_TREE to emptySet(),
        KeymapContext.TERMINAL to emptySet(),
    )
}