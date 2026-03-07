package io.github.numq.haskcore.feature.workspace.core

data class Workspace(
    val path: String = "",
    val name: String? = null,
    val documents: List<WorkspaceDocument> = emptyList(),
    val activeDocumentPath: String? = null,
    val x: Float? = null,
    val y: Float? = null,
    val width: Float? = null,
    val height: Float? = null,
    val isFullscreen: Boolean? = null,
    val ratio: Float = DEFAULT_RATIO
) {
    companion object {
        const val DEFAULT_RATIO = .75f
    }
}