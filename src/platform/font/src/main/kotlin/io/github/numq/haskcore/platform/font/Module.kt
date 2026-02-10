package io.github.numq.haskcore.platform.font

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.module

val monoFontModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { FontManager.loadFont(FontManager.DEFAULT_MONO_FONT) }

        scopedOwner { MonoFont(typeface = get(), size = FontManager.DEFAULT_SIZE, lineSpacing = FontManager.DEFAULT_LINE_SPACING) }
    }
}