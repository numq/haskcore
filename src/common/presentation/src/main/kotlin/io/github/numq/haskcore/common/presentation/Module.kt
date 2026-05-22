package io.github.numq.haskcore.common.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.common.presentation.font.FontManager
import io.github.numq.haskcore.common.presentation.font.SkiaFontManager
import io.github.numq.haskcore.common.presentation.overlay.dialog.file.DesktopFileDialog
import io.github.numq.haskcore.common.presentation.overlay.dialog.file.FileDialog
import io.github.numq.haskcore.common.presentation.theme.editor.AlucardEditorTheme
import io.github.numq.haskcore.common.presentation.theme.editor.DraculaEditorTheme
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import org.koin.dsl.bind
import org.koin.dsl.module

val commonPresentationModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner { SkiaFontManager() } bind FontManager::class

        scopedOwner { DesktopFileDialog() } bind FileDialog::class

        scopedOwner { (isDark: Boolean) ->
            when {
                isDark -> DraculaEditorTheme

                else -> AlucardEditorTheme
            }
        } bind EditorTheme::class
    }
}