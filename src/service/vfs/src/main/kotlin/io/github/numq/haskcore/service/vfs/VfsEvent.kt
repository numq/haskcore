package io.github.numq.haskcore.service.vfs

internal sealed interface VfsEvent {
    val path: String

    data class Created(override val path: String) : VfsEvent

    data class Modified(override val path: String) : VfsEvent

    data class Deleted(override val path: String) : VfsEvent
}