package io.github.numq.haskcore.datasource

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths

internal abstract class BaseJsonDataSource<T> {
    abstract val dirName: String

    abstract val fileName: String

    fun readData(path: String) = runCatching {
        val dataPath = Paths.get(path)

        val metaDirPath = dataPath.resolve(dirName)

        val jsonFilePath = metaDirPath.resolve(fileName)

        if (!Files.exists(jsonFilePath)) return@runCatching null

        try {
            val jsonString = Files.readString(jsonFilePath)

            Json.decodeFromString(jsonString)
        } catch (_: SerializationException) {
            null
        }
    }

    inline fun <reified T> writeData(dataPath: String, data: T) = runCatching {
        val path = Paths.get(dataPath)

        val metaDirPath = path.resolve(dirName)

        if (!Files.exists(metaDirPath)) {
            Files.createDirectories(metaDirPath)
        }

        val jsonFilePath = metaDirPath.resolve(fileName)

        val jsonString = Json.encodeToString<T>(data)

        Files.writeString(jsonFilePath, jsonString)

        Unit
    }

    fun exists(path: String) = runCatching {
        Files.isDirectory(Paths.get(path))
    }
}