package io.github.numq.haskcore.buildsystem.runhaskell

import io.github.numq.haskcore.buildsystem.BuildCommand
import io.github.numq.haskcore.buildsystem.BuildOutput
import io.github.numq.haskcore.buildsystem.BuildSystemService
import kotlinx.coroutines.flow.Flow
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

internal interface RunHaskellBuildSystemService {
    suspend fun isValidScript(path: String): Result<Boolean>

    suspend fun execute(command: BuildCommand.RunHaskell): Result<Flow<BuildOutput>>

    class Default : BuildSystemService(), RunHaskellBuildSystemService {
        override suspend fun isValidScript(path: String) = runCatching {
            Path.of(path).run {
                isRegularFile() && extension == "lhs"
            }
        }

        override suspend fun execute(command: BuildCommand.RunHaskell) = executeBuildCommand(command = command)
    }
}