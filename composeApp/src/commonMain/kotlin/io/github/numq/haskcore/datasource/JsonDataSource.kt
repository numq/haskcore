package io.github.numq.haskcore.datasource

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths

internal open class JsonDataSource<T> {
    fun readJson(path: String) = runCatching {
        val jsonPath = Paths.get(path)

        if (!Files.isReadable(jsonPath)) return@runCatching null

        try {
            val jsonString = Files.readString(jsonPath)

            Json.decodeFromString(jsonString)
        } catch (_: SerializationException) {
            null
        }
    }

    inline fun <reified T> writeJson(path: String, data: T) = runCatching {
        val jsonPath = Paths.get(path)

        val jsonString = Json.encodeToString<T>(data)

        Files.writeString(jsonPath, jsonString)

        Unit
    }

    fun exists(path: String) = runCatching {
        Files.isDirectory(Paths.get(path))
    }
}