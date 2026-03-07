package io.github.numq.haskcore.platform.theme

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.platform.theme.editor.AlucardEditorTheme
import io.github.numq.haskcore.platform.theme.editor.DraculaEditorTheme
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import org.koin.dsl.bind
import org.koin.dsl.module

val themeModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { (isDark: Boolean) ->
            when {
                isDark -> DraculaEditorTheme

                else -> AlucardEditorTheme
            }
        } bind EditorTheme::class
    }
}