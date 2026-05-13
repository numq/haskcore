package io.github.numq.haskcore.common.core.di

import org.koin.core.qualifier.named

object ScopeQualifier {
    sealed interface Type {
        data object Application : Type

        data object Project : Type

        data object Document : Type
    }

    val Application = named<Type.Application>()

    val Project = named<Type.Project>()

    val Document = named<Type.Document>()
}