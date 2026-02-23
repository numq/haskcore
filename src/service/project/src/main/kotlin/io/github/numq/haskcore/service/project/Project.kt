package io.github.numq.haskcore.service.project

data class Project(
    val path: String, val name: String?, val openedDocumentPaths: List<String>, val activeDocumentPath: String?
)