package io.github.numq.haskcore.platform.window

sealed interface Window {
    val id: String

    val title: String?

    val isMinimized: Boolean

    val x: Float?

    val y: Float?

    val width: Float?

    val height: Float?

    data class Fullscreen(
        override val id: String,
        override val title: String?,
        override val isMinimized: Boolean,
        override val x: Float?,
        override val y: Float?,
        override val width: Float?,
        override val height: Float?
    ) : Window

    data class Floating(
        override val id: String,
        override val title: String?,
        override val isMinimized: Boolean,
        override val x: Float?,
        override val y: Float?,
        override val width: Float?,
        override val height: Float?
    ) : Window
}