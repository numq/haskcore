package io.github.numq.haskcore.core.di

sealed interface ScopeContext {
    data class Application(val path: String, val name: String) : ScopeContext

    data class Directory(val path: String) : ScopeContext

    data class File(val path: String, val name: String, val content: String) : ScopeContext
}