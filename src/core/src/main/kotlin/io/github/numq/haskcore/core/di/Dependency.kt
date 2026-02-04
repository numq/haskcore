package io.github.numq.haskcore.core.di

import org.koin.core.Koin
import org.koin.core.qualifier.named

object Dependency {
    private const val APP_DATA = "APPDATA"

    private const val OS_NAME = "os.name"

    private const val USER_HOME = "user.home"

    private const val WIN = "win"

    val globalPath: String // todo String?
        get() = when {
            System.getProperty(OS_NAME).contains(WIN, true) -> System.getenv(APP_DATA) ?: System.getProperty(USER_HOME)

            else -> System.getProperty(USER_HOME)
        }

    fun Koin.getOrCreateScope(context: ScopeContext) = when (context) {
        is ScopeContext.Application -> getOrCreateScope(
            scopeId = context.name, qualifier = named<ScopeQualifier.Application>()
        ).apply {
            declare<ScopeContext.Application>(instance = context)
        }

        is ScopeContext.Directory -> getOrCreateScope(
            scopeId = context.path, qualifier = named<ScopeQualifier.Directory>()
        ).apply {
            declare<ScopeContext.Directory>(instance = context)
        }

        is ScopeContext.File -> getOrCreateScope(
            scopeId = context.path, qualifier = named<ScopeQualifier.File>()
        ).apply {
            declare<ScopeContext.File>(instance = context)
        }
    }
}