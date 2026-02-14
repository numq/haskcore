package io.github.numq.haskcore.platform.font

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.module

val fontModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { FontManager() }

        scopedOwner { (size: Float, lineSpacing: Float) ->
            LogoFont(
                typeface = get<FontManager>().loadFont(FontResources.LOGO), size = size, lineSpacing = lineSpacing
            )
        }

        scopedOwner { (size: Float, lineSpacing: Float) ->
            MonoFont(
                typeface = get<FontManager>().loadFont(FontResources.MONO), size = size, lineSpacing = lineSpacing
            )
        }

        scopedOwner { (size: Float, lineSpacing: Float) ->
            EditorFont(
                typeface = get<FontManager>().loadFont(FontResources.MONO), size = size, lineSpacing = lineSpacing
            )
        }
    }
}