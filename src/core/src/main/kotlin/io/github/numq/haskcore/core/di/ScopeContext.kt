package io.github.numq.haskcore.core.di

sealed interface ScopeContext {
    val path: String

    @JvmInline
    value class Application(override val path: String) : ScopeContext

    @JvmInline
    value class Project(override val path: String) : ScopeContext

    @JvmInline
    value class Document(override val path: String) : ScopeContext
}