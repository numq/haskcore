package io.github.numq.haskcore.core.di

sealed interface ScopeQualifier {
    object Application : ScopeQualifier

    object Directory : ScopeQualifier

    object File : ScopeQualifier
}