package io.github.numq.haskcore.feature.workspace.core

data class Workspace(
    val path: String = "",
    val name: String? = null,
    val documents: List<WorkspaceDocument> = emptyList(),
    val activeDocument: WorkspaceDocument? = null,
    val x: Float = DEFAULT_X,
    val y: Float = DEFAULT_Y,
    val width: Float = DEFAULT_WIDTH,
    val height: Float = DEFAULT_HEIGHT,
    val isFullscreen: Boolean = false,
    val verticalRatio: Float = DEFAULT_VERTICAL_RATIO,
    val shelf: Shelf = Shelf(),
) {
    companion object {
        const val DEFAULT_X = 0f

        const val DEFAULT_Y = 0f

        const val DEFAULT_WIDTH = 800f

        const val DEFAULT_HEIGHT = 600f

        const val DEFAULT_VERTICAL_RATIO = .75f
    }
}