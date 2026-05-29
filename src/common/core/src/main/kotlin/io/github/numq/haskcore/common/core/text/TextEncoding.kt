package io.github.numq.haskcore.common.core.text

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

sealed interface TextEncoding {
    val charset: Charset

    val bomSize: Int

    data object UTF8 : TextEncoding {
        override val charset: Charset = StandardCharsets.UTF_8

        override val bomSize = 0
    }

    data object UTF16LE : TextEncoding {
        override val charset: Charset = StandardCharsets.UTF_16LE

        override val bomSize = 2
    }

    data object UTF16BE : TextEncoding {
        override val charset: Charset = StandardCharsets.UTF_16BE

        override val bomSize = 2
    }

    data object UTF32LE : TextEncoding {
        override val charset: Charset = StandardCharsets.UTF_32LE

        override val bomSize = 4
    }

    data object UTF32BE : TextEncoding {
        override val charset: Charset = StandardCharsets.UTF_32BE

        override val bomSize = 4
    }

    companion object {
        fun fromCharset(charset: Charset) = when (charset) {
            StandardCharsets.UTF_8 -> UTF8

            StandardCharsets.UTF_16LE -> UTF16LE

            StandardCharsets.UTF_16BE -> UTF16BE

            StandardCharsets.UTF_32LE -> UTF32LE

            StandardCharsets.UTF_32BE -> UTF32BE

            else -> UTF8
        }

        fun detectFromBOM(bytes: ByteArray) = when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> UTF16LE

            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> UTF16BE

            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> UTF8

            else -> UTF8
        }
    }
}