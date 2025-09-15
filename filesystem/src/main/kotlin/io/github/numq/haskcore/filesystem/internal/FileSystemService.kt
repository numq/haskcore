package io.github.numq.haskcore.filesystem.internal

import io.github.numq.haskcore.filesystem.exception.FileSystemException
import io.github.numq.haskcore.filesystem.internal.FileSystemItem.Directory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
interface FileSystemService {
    suspend fun exists(path: String): Result<Boolean>

    suspend fun isFile(path: String): Result<Boolean>

    suspend fun isDirectory(path: String): Result<Boolean>

    suspend fun listDirectory(path: String, recursive: Boolean): Result<List<FileSystemItem>>

    suspend fun createDirectory(path: String): Result<Unit>

    suspend fun createFile(path: String, text: String): Result<Unit>

    suspend fun createFile(path: String, bytes: ByteArray): Result<Unit>

    suspend fun rename(path: String, name: String): Result<Unit>

    suspend fun move(fromPath: String, toPath: String, overwrite: Boolean): Result<Unit>

    suspend fun copy(fromPath: String, toPath: String, overwrite: Boolean): Result<Unit>

    suspend fun delete(path: String): Result<Unit>

    class Default : FileSystemService {
        private fun getCreationTime(file: File) = runCatching {
            val attr = Files.readAttributes(file.toPath(), "creationTime")

            Instant.fromEpochMilliseconds((attr["creationTime"] as FileTime).toMillis())
        }.getOrDefault(Instant.fromEpochMilliseconds(file.lastModified()))

        private fun getPermissions(file: File) = buildString {
            append(if (file.canRead()) "r" else "-")
            append(if (file.canWrite()) "w" else "-")
            append(if (file.canExecute()) "x" else "-")
        }

        private fun getSize(file: File) = when {
            file.isDirectory -> file.walk().filter(File::isFile).fold(0L) { size, f -> size + f.length() }

            else -> file.length()
        }

        private fun createFile(file: File) = with(file) {
            FileSystemItem.File(
                name = name,
                path = path,
                size = getSize(this),
                isHidden = isHidden,
                isReadOnly = !canWrite(),
                permissions = getPermissions(this),
                createdAt = getCreationTime(this),
                accessedAt = Instant.fromEpochMilliseconds(lastModified()),
                modifiedAt = Instant.fromEpochMilliseconds(lastModified())
            )
        }

        private fun createDirectory(file: File) = with(file) {
            Directory(
                name = name,
                path = path,
                size = getSize(this),
                isHidden = isHidden,
                isReadOnly = !canWrite(),
                permissions = getPermissions(this),
                createdAt = getCreationTime(this),
                accessedAt = Instant.fromEpochMilliseconds(lastModified()),
                modifiedAt = Instant.fromEpochMilliseconds(lastModified()),
                children = listFiles()?.map(::createFromFile) ?: emptyList()
            )
        }

        private fun createFromFile(file: File): FileSystemItem = when {
            file.isDirectory -> createDirectory(file = file)

            else -> createFile(file = file)
        }

        override suspend fun exists(path: String) = runCatching {
            File(path).exists()
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to check existence of '$path': ${throwable.message}")
        }

        override suspend fun isFile(path: String) = runCatching {
            val file = File(path)

            file.exists() && file.isFile
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to check if '$path' is file: ${throwable.message}")
        }

        override suspend fun isDirectory(path: String): Result<Boolean> {
            return runCatching {
                val file = File(path)

                file.exists() && file.isDirectory
            }.recoverCatching { throwable ->
                throw FileSystemException("Failed to check if '$path' is directory: ${throwable.message}")
            }
        }

        override suspend fun listDirectory(path: String, recursive: Boolean): Result<List<FileSystemItem>> =
            runCatching {
                val directory = File(path)

                if (!directory.exists()) {
                    throw FileSystemException("Directory '$path' does not exist")
                }

                if (!directory.isDirectory) {
                    throw FileSystemException("'$path' is not a directory")
                }

                directory.listFiles()?.map(::createFromFile) ?: emptyList()
            }.recoverCatching { throwable ->
                throw FileSystemException("Failed to list directory '$path': ${throwable.message}")
            }

        override suspend fun createDirectory(path: String) = runCatching {
            if (!File(path).mkdirs()) {
                throw IOException("Failed to create directory")
            }
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to create directory '$path': ${throwable.message}")
        }

        override suspend fun createFile(path: String, text: String) = runCatching {
            val file = File(path)

            file.parentFile?.mkdirs()

            file.writeText(text)
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to create file '$path': ${throwable.message}")
        }

        override suspend fun createFile(path: String, bytes: ByteArray) = runCatching {
            val file = File(path)

            file.parentFile?.mkdirs()

            file.writeBytes(bytes)
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to create file '$path': ${throwable.message}")
        }

        override suspend fun rename(path: String, name: String) = runCatching {
            val srcFile = File(path)

            if (!srcFile.exists()) {
                throw FileSystemException("Path '$path' does not exist")
            }

            val dstFile = File(srcFile.parent, name)

            if (dstFile.exists()) {
                val target = when {
                    dstFile.isDirectory -> "Directory"

                    else -> "File"
                }

                throw FileSystemException("$target '$name' already exists in directory '${dstFile.parent}'")
            }

            if (!srcFile.renameTo(dstFile)) {
                throw IOException("Failed to rename")
            }
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to rename '$path' to '$name': ${throwable.message}")
        }

        override suspend fun move(fromPath: String, toPath: String, overwrite: Boolean) = runCatching {
            val srcFile = File(fromPath)

            val dstFile = File(toPath)

            if (dstFile.exists() && !overwrite) {
                throw FileSystemException("File already exists: $toPath")
            }

            when {
                srcFile.isDirectory -> {
                    srcFile.copyRecursively(dstFile, overwrite = overwrite)

                    srcFile.deleteRecursively()
                }

                else -> {
                    srcFile.copyTo(dstFile, overwrite = overwrite)

                    srcFile.delete()
                }
            }

            Unit
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to move $fromPath to $toPath: ${throwable.message}")
        }

        override suspend fun copy(
            fromPath: String,
            toPath: String,
            overwrite: Boolean,
        ) = runCatching {
            val srcFile = File(fromPath)

            val dstFile = File(toPath)

            if (dstFile.exists() && !overwrite) {
                throw FileSystemException("File already exists: $toPath")
            }

            when {
                srcFile.isDirectory -> srcFile.copyRecursively(dstFile, overwrite = overwrite)

                else -> srcFile.copyTo(dstFile, overwrite = overwrite)
            }

            Unit
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to copy $fromPath to $toPath: ${throwable.message}")
        }

        override suspend fun delete(path: String) = runCatching {
            val file = File(path)

            if (!file.exists()) {
                throw FileSystemException("Path '$path' does not exist")
            }

            when {
                file.isDirectory -> file.deleteRecursively()

                !file.delete() -> throw IOException("Failed to delete file")
            }

            Unit
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to delete '$path': ${throwable.message}")
        }
    }
}