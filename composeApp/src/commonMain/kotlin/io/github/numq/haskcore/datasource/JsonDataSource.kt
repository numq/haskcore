package io.github.numq.haskcore.datasource

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

internal open class JsonDataSource<T> {
    fun readJson(path: String) = runCatching {
        val jsonPath = Path.of(path)

        if (!Files.isReadable(jsonPath)) return@runCatching null

        try {
            val jsonString = Files.readString(jsonPath)

            Json.decodeFromString(jsonString)
        } catch (_: SerializationException) {
            null
        }
    }

    inline fun <reified T> writeJson(path: String, data: T) = runCatching {
        val jsonPath = Path.of(path)

        val jsonString = Json.encodeToString<T>(data)

        Files.writeString(jsonPath, jsonString)

        Unit
    }

    fun exists(path: String) = runCatching {
        Path.of(path).isDirectory()
    }
}