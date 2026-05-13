package io.github.numq.haskcore.service.project

internal fun ProjectData.toProject(path: String) = Project(
    path = path, name = name, openedDocumentPaths = openedDocumentPaths, activeDocumentPath = activeDocumentPath
)