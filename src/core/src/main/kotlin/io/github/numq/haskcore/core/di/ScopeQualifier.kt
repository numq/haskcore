package io.github.numq.haskcore.core.di

import org.koin.core.qualifier.named

object ScopeQualifier {
    private const val APPLICATION_PATH = "APPLICATION_PATH"

    private const val PROJECT_PATH = "PROJECT_PATH"

    private const val DOCUMENT_PATH = "DOCUMENT_PATH"

    val Application = named(APPLICATION_PATH)

    val Project = named(PROJECT_PATH)

    val Document = named(DOCUMENT_PATH)
}