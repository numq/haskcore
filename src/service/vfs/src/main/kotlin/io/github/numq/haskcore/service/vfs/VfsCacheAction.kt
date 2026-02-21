package io.github.numq.haskcore.service.vfs

internal sealed interface VfsCacheAction {
    data class SetDirectory(val path: String, val files: List<VirtualFile>) : VfsCacheAction

    data class RemoveDirectory(val path: String) : VfsCacheAction

    data class UpdateEntry(val parentPath: String, val event: VfsEvent, val newFile: VirtualFile?) : VfsCacheAction
}