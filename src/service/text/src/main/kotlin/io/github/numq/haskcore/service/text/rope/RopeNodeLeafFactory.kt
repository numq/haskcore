package io.github.numq.haskcore.service.text.rope

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal class RopeNodeLeafFactory(
    enablePooling: Boolean, private val charset: Charset, private val maxLeafSize: Int = 8192
) {
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
                    eldest: MutableMap.MutableEntry<String, String>
                ) = size > maxEntries / 3
            })

        private val largeStringsPool =
            Collections.synchronizedMap(object : LinkedHashMap<String, String>(maxEntries / 3, .75f, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<String, String>
                ) = size > maxEntries / 3
            })

        fun stats() = "Tiny: ${tinyStringsPool.size}, Small: ${smallStringsPool.size}, Large: ${largeStringsPool.size}"

        fun getOrCreate(content: String): String = when {
            content.length <= TINY_STRING_THRESHOLD -> tinyStringsPool.getOrPut(content) { content }

            content.length <= SMALL_STRING_THRESHOLD -> smallStringsPool.getOrPut(content) { content }

            else -> largeStringsPool.getOrPut(content) { content }
        }

        fun clear() {
            tinyStringsPool.clear()

            smallStringsPool.clear()

            largeStringsPool.clear()
        }
    }

    private data class StringStats(
        val bytes: Int, val newlineCount: Int, val maxLineLen: Int, val prefixLen: Int, val suffixLen: Int
    )

    private val leafCache = object : LinkedHashMap<String, RopeNode>(
        DEFAULT_LEAF_CACHE_SIZE / 2, .75f, true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, RopeNode>
        ) = size > DEFAULT_LEAF_CACHE_SIZE
    }

    private val stringPool = if (enablePooling) StringPool() else null

    private fun calculateStringStats(content: String): StringStats {
        var newlineCount = 0

        var maxLineLen = 0

        var currentLineLen = 0

        val len = content.length

        var prefixLen = len

        var foundFirstNewline = false

        for (i in 0 until len) {
            when (content[i]) {
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

        val lastNewlineIndex = content.lastIndexOf('\n')

        val suffixLen = if (lastNewlineIndex == -1) len else len - (lastNewlineIndex + 1)

        val bytes = when (charset) {
            StandardCharsets.UTF_32, StandardCharsets.UTF_32BE, StandardCharsets.UTF_32LE -> content.length * 4

            StandardCharsets.UTF_16, StandardCharsets.UTF_16BE, StandardCharsets.UTF_16LE -> content.length * 2

            StandardCharsets.UTF_8 -> {
                var utf8Size = 0

                var i = 0

                while (i < content.length) {
                    val c = content[i].code

                    when {
                        c <= 0x7F -> utf8Size += 1

                        c <= 0x7FF -> utf8Size += 2

                        Character.isHighSurrogate(content[i]) && i + 1 < len && Character.isLowSurrogate(content[i + 1]) -> {
                            utf8Size += 4

                            i++
                        }

                        else -> utf8Size += 3
                    }

                    i++
                }

                utf8Size
            }

            else -> content.toByteArray(charset).size
        }

        return StringStats(bytes, newlineCount, maxLineLen, prefixLen, suffixLen)
    }

    private fun splitLargeContent(content: String): RopeNode {
        val parts = ArrayList<RopeNode>(content.length / maxLeafSize + 2)

        var start = 0

        val len = content.length

        while (start < len) {
            var end = min(start + maxLeafSize, len)

            if (end < len) {
                var newlinePos = -1

                val searchLimit = max(start, end - SEARCH_BACK_LIMIT)

                for (i in end - 1 downTo searchLimit) {
                    if (content[i] == '\n') {
                        newlinePos = i + 1

                        break
                    }
                }

                if (newlinePos != -1) end = newlinePos
            }

            val part = content.substring(start, end)

            parts.add(createLeaf(part))

            start = end
        }

        return buildBalancedTree(parts)
    }

    private fun buildBalancedTree(nodes: List<RopeNode>): RopeNode {
        fun build(start: Int, end: Int): RopeNode = when (val count = end - start) {
            0 -> RopeNode.Empty

            1 -> nodes[start]

            else -> {
                val mid = start + count / 2

                val left = build(start, mid)

                val right = build(mid, end)

                RopeNode.Branch(left, right, RopeNode.Color.BLACK)
            }
        }

        return build(0, nodes.size)
    }

    @Synchronized
    fun getStats() = stringPool?.stats()

    @Synchronized
    fun createLeaf(content: String): RopeNode {
        if (content.isEmpty()) return RopeNode.Empty

        if (content.length > maxLeafSize) return splitLargeContent(content)

        val pooledContent = stringPool?.getOrCreate(content) ?: content

        leafCache[pooledContent]?.let { return it }

        val stats = calculateStringStats(pooledContent)

        val leaf = RopeNode.Leaf(
            content = pooledContent,
            byteCount = stats.bytes,
            lineBreakCount = stats.newlineCount,
            prefixLineLength = stats.prefixLen,
            suffixLineLength = stats.suffixLen,
            maxLineLength = stats.maxLineLen,
            color = RopeNode.Color.BLACK
        )

        leafCache[pooledContent] = leaf

        return leaf
    }

    @Synchronized
    fun clearCache() {
        leafCache.clear()

        stringPool?.clear()
    }
}