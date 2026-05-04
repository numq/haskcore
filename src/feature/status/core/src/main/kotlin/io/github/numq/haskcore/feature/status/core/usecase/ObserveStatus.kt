package io.github.numq.haskcore.feature.status.core.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import io.github.numq.haskcore.api.project.ProjectApi
import io.github.numq.haskcore.api.toolchain.Tool
import io.github.numq.haskcore.api.toolchain.Toolchain
import io.github.numq.haskcore.api.toolchain.ToolchainService
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.status.core.Status
import io.github.numq.haskcore.feature.status.core.StatusService
import io.github.numq.haskcore.feature.status.core.StatusTool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveStatus(
    private val statusService: StatusService,
    private val projectApi: ProjectApi,
    private val toolchainService: ToolchainService,
) : UseCase<Unit, Flow<Status>> {
    private fun Either<Throwable, Tool>.toStatus(): StatusTool = fold(ifLeft = { throwable ->
        when (throwable) {
            is NoSuchElementException -> StatusTool.NotFound

            else -> StatusTool.Error(throwable = throwable)
        }
    }, ifRight = { tool ->
        StatusTool.Ready(path = tool.path, version = tool.version)
    })

    override suspend fun Raise<Throwable>.execute(input: Unit) = toolchainService.toolchain.map { toolchain ->
        when (toolchain) {
            is Toolchain.Scanning -> Status(
                ghc = StatusTool.Scanning,
                cabal = StatusTool.Scanning,
                stack = StatusTool.Scanning,
                hls = StatusTool.Scanning
            )

            is Toolchain.Detected -> Status(
                ghc = toolchain.ghc.toStatus(),
                cabal = toolchain.cabal.toStatus(),
                stack = toolchain.stack.toStatus(),
                hls = toolchain.hls.toStatus()
            )

            is Toolchain.NotFound -> Status(
                ghc = StatusTool.NotFound,
                cabal = StatusTool.NotFound,
                stack = StatusTool.NotFound,
                hls = StatusTool.NotFound
            )

            is Toolchain.Error -> {
                val error = StatusTool.Error(throwable = toolchain.throwable)

                Status(ghc = error, cabal = error, stack = error, hls = error)
            }
        }
    }.distinctUntilChanged().combine(
        flow = projectApi.projectDto, transform = { status, project ->
            when (val activeDocumentPath = project.activeDocumentPath) {
                null -> status

                else -> {
                    val pathSegments = statusService.getPathSegments(
                        rootPath = project.path, filePath = activeDocumentPath
                    ).bind()

                    status.copy(pathSegments = pathSegments)
                }
            }
        })
}