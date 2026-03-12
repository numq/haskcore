package io.github.numq.haskcore.core.di

import org.koin.core.qualifier.named

object ScopeQualifier {
    val Application = named("APPLICATION_PATH")

    val Project = named("PROJECT_PATH")

    val Document = named("DOCUMENT_PATH")
}