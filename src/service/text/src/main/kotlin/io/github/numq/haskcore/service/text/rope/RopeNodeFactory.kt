package io.github.numq.haskcore.service.text.rope

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal class RopeNodeFactory(
    enablePooling: Boolean, private val charset: Charset, private val maxLeafSize: Int = 8192,
) : NodeFactory {
    private companion object {
        const val DEFAULT_LEAF_CACHE_SIZE = 2048

        const val DEFAULT_POOL_SIZE = 4096

        const val SEARCH_BACK_LIMIT = 128

        const val SMALL_STRING_THRESHOLD = 64

        const val TINY_STRING_THRESHOLD = 8
    }

    private class StringPool(private val maxEntries: Int = DEFAULT_POOL_SIZE) {
        private val tinyStringsPool =
            Collections.synchronizedMap(object : LinkedHashMap<String, String>(128, .75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>) = size > 256
            })

        private val smallStringsPool =
            Collections.synchronizedMap(object : LinkedHashMap<String, String>(maxEntries / 3, .75f, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<String, String>,
                ) = size > maxEntries / 3
            })

        private val largeStringsPool =
            Collections.synchronizedMap(object : LinkedHashMap<String, String>(maxEntries / 3, .75f, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<String, String>,
                ) = size > maxEntries / 3
            })

        fun stats() = "Tiny: ${tinyStringsPool.size}, Small: ${smallStringsPool.size}, Large: ${largeStringsPool.size}"

        fun getOrCreate(text: String): String = when {
            text.length <= TINY_STRING_THRESHOLD -> tinyStringsPool.getOrPut(text) { text }

            text.length <= SMALL_STRING_THRESHOLD -> smallStringsPool.getOrPut(text) { text }

            else -> largeStringsPool.getOrPut(text) { text }
        }

        fun clear() {
            tinyStringsPool.clear()

            smallStringsPool.clear()

            largeStringsPool.clear()
        }
    }

    private data class StringStats(
        val bytes: Int, val newlineCount: Int, val maxLineLen: Int, val prefixLen: Int, val suffixLen: Int,
    )

    private val leafCache = object : LinkedHashMap<String, Node>(
        DEFAULT_LEAF_CACHE_SIZE / 2, .75f, true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, Node>,
        ) = size > DEFAULT_LEAF_CACHE_SIZE
    }

    private val stringPool = if (enablePooling) StringPool() else null

    private fun calculateStringStats(text: String): StringStats {
        var newlineCount = 0

        var maxLineLen = 0

        var currentLineLen = 0

        val len = text.length

        var prefixLen = len

        var foundFirstNewline = false

        for (i in 0 until len) {
            when (text[i]) {
                '\n' -> {
                    if (!foundFirstNewline) {
                        prefixLen = i

                        foundFirstNewline = true
                    }

                    if (currentLineLen > maxLineLen) maxLineLen = currentLineLen

                    newlineCount++

                    currentLineLen = 0
                }

                else -> currentLineLen++
            }
        }

        if (currentLineLen > maxLineLen) maxLineLen = currentLineLen

        val lastNewlineIndex = text.lastIndexOf('\n')

        val suffixLen = if (lastNewlineIndex == -1) len else len - (lastNewlineIndex + 1)

        val bytes = when (charset) {
            StandardCharsets.UTF_32, StandardCharsets.UTF_32BE, StandardCharsets.UTF_32LE -> text.length * 4

            StandardCharsets.UTF_16, StandardCharsets.UTF_16BE, StandardCharsets.UTF_16LE -> text.length * 2

            StandardCharsets.UTF_8 -> {
                var utf8Size = 0

                var i = 0

                while (i < text.length) {
                    val c = text[i].code

                    when {
                        c <= 0x7F -> utf8Size += 1

                        c <= 0x7FF -> utf8Size += 2

                        Character.isHighSurrogate(text[i]) && i + 1 < len && Character.isLowSurrogate(text[i + 1]) -> {
                            utf8Size += 4

                            i++
                        }

                        else -> utf8Size += 3
                    }

                    i++
                }

                utf8Size
            }

            else -> text.toByteArray(charset).size
        }

        return StringStats(bytes, newlineCount, maxLineLen, prefixLen, suffixLen)
    }

    private fun splitLargeText(text: String): Node {
        val parts = ArrayList<Node>(text.length / maxLeafSize + 2)

        var start = 0

        val len = text.length

        while (start < len) {
            var end = min(start + maxLeafSize, len)

            if (end < len) {
                var newlinePos = -1

                val searchLimit = max(start, end - SEARCH_BACK_LIMIT)

                for (i in end - 1 downTo searchLimit) {
                    if (text[i] == '\n') {
                        newlinePos = i + 1

                        break
                    }
                }

                if (newlinePos != -1) end = newlinePos
            }

            val part = text.substring(start, end)

            parts.add(create(part))

            start = end
        }

        return buildBalancedTree(parts)
    }

    private fun buildBalancedTree(nodes: List<Node>): Node {
        fun build(start: Int, end: Int): Node = when (val count = end - start) {
            0 -> Node.Empty

            1 -> nodes[start]

            else -> {
                val mid = start + count / 2

                val left = build(start, mid)

                val right = build(mid, end)

                Node.Branch(left, right, Node.Color.BLACK)
            }
        }

        return build(0, nodes.size)
    }

    @Synchronized
    fun getStats() = stringPool?.stats()

    override fun create(text: String): Node {
        if (text.isEmpty()) return Node.Empty

        if (text.length > maxLeafSize) return splitLargeText(text)

        val pooledText = stringPool?.getOrCreate(text) ?: text

        leafCache[pooledText]?.let { return it }

        val stats = calculateStringStats(pooledText)

        val leaf = Node.Leaf(
            text = pooledText,
            byteCount = stats.bytes,
            lineBreakCount = stats.newlineCount,
            prefixLineLength = stats.prefixLen,
            suffixLineLength = stats.suffixLen,
            maxLineLength = stats.maxLineLen,
            color = Node.Color.BLACK
        )

        leafCache[pooledText] = leaf

        return leaf
    }

    @Synchronized
    fun clearCache() {
        leafCache.clear()

        stringPool?.clear()
    }
}