package io.github.numq.haskcore.feature.workspace.core

data class Workspace(
    val path: String = "",
    val name: String? = null,
    val documents: List<WorkspaceDocument> = emptyList(),
    val activeDocument: WorkspaceDocument? = null,
    val x: Float? = null,
    val y: Float? = null,
    val width: Float? = null,
    val height: Float? = null,
    val isFullscreen: Boolean? = null,
    val verticalRatio: Float = DEFAULT_VERTICAL_RATIO,
    val shelf: Shelf = Shelf(),
) {
    companion object {
        const val DEFAULT_VERTICAL_RATIO = .75f
    }
}