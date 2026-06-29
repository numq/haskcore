package io.github.numq.haskcore.feature.explorer.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.explorer.core.ExplorerNode
import io.github.numq.haskcore.service.vfs.VfsService
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class CreateFileNode(private val vfsService: VfsService) : UseCase.Command<CreateFileNode.Input> {
    data class Input(val node: ExplorerNode, val name: String)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        vfsService.create(
            path = Path(node.path, name).absolutePathString(), isDirectory = false
        ).bind()
    }
}