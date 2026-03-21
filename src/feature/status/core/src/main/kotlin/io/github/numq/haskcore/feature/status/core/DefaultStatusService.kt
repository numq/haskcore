package io.github.numq.haskcore.feature.status.core

import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.pathString

internal class DefaultStatusService : StatusService {
    override suspend fun getPathSegments(rootPath: String, filePath: String) = Either.catch {
        withContext(Dispatchers.IO) {
            val root = Path.of(rootPath)

            val file = Path.of(filePath)

            buildList {
                add(rootPath)

                val parentPath = file.parent

                if (parentPath != null) {
                    val relative = root.relativize(parentPath)

                    if (relative.pathString.isNotEmpty() && relative.pathString != ".") {
                        var current = root

                        relative.forEach { part ->
                            current = current.resolve(part)

                            add(current.pathString)
                        }
                    }
                }

                if (filePath != rootPath) {
                    add(filePath)
                }
            }
        }
    }
}