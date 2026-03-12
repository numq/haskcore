package io.github.numq.haskcore.core.di

sealed interface ScopeQualifierType {
    object Application : ScopeQualifierType

    object Project : ScopeQualifierType

    object Document : ScopeQualifierType
}