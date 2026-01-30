package io.github.numq.haskcore.platform.core.di

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
}