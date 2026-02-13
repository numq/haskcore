package io.github.numq.haskcore.core.di

sealed interface ScopeQualifier {
    object Application : ScopeQualifier

    object Project : ScopeQualifier

    object Document : ScopeQualifier
}