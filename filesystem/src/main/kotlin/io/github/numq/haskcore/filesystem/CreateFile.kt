package io.github.numq.haskcore.filesystem

import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.filesystem.internal.FileSystemService

class CreateFile(private val fileSystem: FileSystemService) : UseCase<CreateFile.Input, Unit> {
    sealed interface Input {
        val path: String

        data class Text(override val path: String, val text: String) : Input

        data class Bytes(override val path: String, val bytes: ByteArray) : Input {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Bytes

                if (path != other.path) return false
                if (!bytes.contentEquals(other.bytes)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = path.hashCode()
                result = 31 * result + bytes.contentHashCode()
                return result
            }
        }
    }

    override suspend fun execute(input: Input) = with(input) {
        when (this) {
            is Input.Text -> fileSystem.createFile(path = path, text = text)

            is Input.Bytes -> fileSystem.createFile(path = path, bytes = bytes)
        }
    }
}