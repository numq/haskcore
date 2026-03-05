package io.github.numq.haskcore.core.di

import org.koin.core.qualifier.named

object ScopePath {
    val Application = named("APPLICATION_PATH")

    val Project = named("PROJECT_PATH")

    val Document = named("DOCUMENT_PATH")
}