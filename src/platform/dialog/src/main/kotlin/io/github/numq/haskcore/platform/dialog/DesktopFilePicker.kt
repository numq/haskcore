package io.github.numq.haskcore.platform.dialog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.UIManager

internal class DesktopFilePicker : FilePicker {
    override suspend fun pickDirectory(title: String) = withContext(Dispatchers.IO) {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY

            dialogTitle = title

            isAcceptAllFileFilterUsed = false
        }

        when (chooser.showOpenDialog(null)) {
            JFileChooser.APPROVE_OPTION -> chooser.selectedFile?.absolutePath

            else -> null
        }
    }
}