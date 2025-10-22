package io.github.numq.haskcore.explorer

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths

internal interface ExplorerDataSource {
    suspend fun readExplorer(path: String): Result<Explorer?>

    suspend fun writeExplorer(explorer: Explorer): Result<Unit>

    suspend fun exists(path: String): Result<Boolean>

    class Default : ExplorerDataSource {
        private companion object {
            const val DIR_NAME = ".haskcore"

            const val JSON_NAME = "explorer.json"
        }

        override suspend fun readExplorer(path: String) = runCatching {
            val explorerPath = Paths.get(path)

            val metaDirPath = explorerPath.resolve(DIR_NAME)

            val explorerJsonPath = metaDirPath.resolve(JSON_NAME)

            if (!Files.exists(explorerJsonPath)) return@runCatching null

            try {
                val jsonString = Files.readString(explorerJsonPath)

                Json.decodeFromString<Explorer>(jsonString)
            } catch (_: SerializationException) {
                null
            }
        }

        override suspend fun writeExplorer(explorer: Explorer) = runCatching {
            val explorerPath = Paths.get(explorer.path)

            val metaDirPath = explorerPath.resolve(DIR_NAME)

            if (!Files.exists(metaDirPath)) {
                Files.createDirectories(metaDirPath)
            }

            val explorerJsonPath = metaDirPath.resolve(JSON_NAME)

            val jsonString = Json.encodeToString(explorer)

            Files.writeString(explorerJsonPath, jsonString)

            Unit
        }

        override suspend fun exists(path: String) = runCatching {
            Files.isDirectory(Paths.get(path))
        }
    }
}