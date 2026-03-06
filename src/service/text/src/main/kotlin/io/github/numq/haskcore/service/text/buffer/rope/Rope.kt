package io.github.numq.haskcore.service.text.buffer.rope

import kotlinx.atomicfu.atomic
import java.lang.ref.SoftReference
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

internal class Rope private constructor(
    private val root: RopeNode,
    private val charset: Charset,
    private val ropeNodeLeafFactory: RopeNodeLeafFactory,
    val textVersion: Long = 0L
) {
    constructor(
        initialText: String = "",
        charset: Charset = StandardCharsets.UTF_8,
        ropeNodeLeafFactory: RopeNodeLeafFactory = RopeNodeLeafFactory(enablePooling = true, charset = charset)
    ) : this(
        root = if (initialText.isEmpty()) RopeNode.Empty else ropeNodeLeafFactory.createLeaf(content = initialText),
        charset = charset,
        ropeNodeLeafFactory = ropeNodeLeafFactory,
        textVersion = 0L
    )

    private val lineOffsetCache = Collections.synchronizedMap(object : LinkedHashMap<Int, Int>(512, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Int>) = size > 1024
    })

    private val byteOffsetCache = Collections.synchronizedMap(object : LinkedHashMap<Int, Int>(512, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Int>) = size > 1024
    })

    private var cachedTextRef: SoftReference<String>? = null

    private val cachedTextVersion = atomic(-1L)

    private fun getBOMSize(charset: Charset) = when (charset) {
        StandardCharsets.UTF_32 -> 4

        StandardCharsets.UTF_16 -> 2

        else -> 0
    }

    val totalBytes: Int get() = root.byteCount + getBOMSize(charset)

    val totalChars: Int get() = root.charCount

    val totalLines: Int get() = root.lineBreakCount + 1

    val maxLineLength: Int get() = root.maxLineLength

    private fun createLeaf(content: String) = ropeNodeLeafFactory.createLeaf(content)

    private fun isRed(node: RopeNode) = node.color == RopeNode.Color.RED

    private fun rotateLeft(node: RopeNode.Branch): RopeNode.Branch {
        val x = node.right

        if (x !is RopeNode.Branch) return node

        return RopeNode.Branch(
            left = RopeNode.Branch(node.left, x.left, node.color), right = x.right, color = x.color
        )
    }

    private fun rotateRight(node: RopeNode.Branch): RopeNode.Branch {
        val x = node.left

        if (x !is RopeNode.Branch) return node

        return RopeNode.Branch(
            left = x.left, right = RopeNode.Branch(x.right, node.right, node.color), color = x.color
        )
    }

    private fun flipColors(node: RopeNode.Branch): RopeNode.Branch {
        fun flip(color: RopeNode.Color) = when (color) {
            RopeNode.Color.BLACK -> RopeNode.Color.RED

            else -> RopeNode.Color.BLACK
        }

        val newLeft = when (val left = node.left) {
            is RopeNode.Leaf -> left.copy(color = flip(left.color))

            is RopeNode.Branch -> left.copy(color = flip(left.color))

            RopeNode.Empty -> left
        }

        val newRight = when (val right = node.right) {
            is RopeNode.Leaf -> right.copy(color = flip(right.color))

            is RopeNode.Branch -> right.copy(color = flip(right.color))

            RopeNode.Empty -> right
        }

        return node.copy(left = newLeft, right = newRight, color = flip(node.color))
    }

    private fun quickBalance(node: RopeNode): RopeNode {
        if (node !is RopeNode.Branch) return node

        var current = node

        if (isRed(current.right) && !isRed(current.left) && current.right is RopeNode.Branch) {
            current = rotateLeft(current)
        }

        if (isRed(current.left) && current.left is RopeNode.Branch && isRed(current.left.left)) {
            current = rotateRight(current)
        }

        if (isRed(current.left) && isRed(current.right)) {
            current = flipColors(current)
        }

        return current
    }

    private fun splitAt(offset: Int, node: RopeNode): Pair<RopeNode, RopeNode> = when {
        offset <= 0 -> Pair(RopeNode.Empty, node)

        offset >= node.charCount -> Pair(node, RopeNode.Empty)

        node is RopeNode.Leaf -> {
            val leftContent = node.content.substring(0, offset)

            val rightContent = node.content.substring(offset)

            Pair(createLeaf(leftContent), createLeaf(rightContent))
        }

        node is RopeNode.Branch -> when {
            offset < node.left.charCount -> {
                val (leftLeft, leftRight) = splitAt(offset, node.left)

                Pair(leftLeft, concat(leftRight, node.right))
            }

            else -> {
                val (rightLeft, rightRight) = splitAt(offset - node.left.charCount, node.right)

                Pair(concat(node.left, rightLeft), rightRight)
            }
        }

        else -> Pair(RopeNode.Empty, RopeNode.Empty)
    }

    private fun concat(left: RopeNode, right: RopeNode): RopeNode {
        if (left === RopeNode.Empty) return right

        if (right === RopeNode.Empty) return left

        if (left is RopeNode.Leaf && right is RopeNode.Leaf && left.length + right.length <= 8192) {
            return createLeaf(left.content + right.content)
        }

        return quickBalance(RopeNode.Branch(left, right, RopeNode.Color.RED))
    }

    private fun needsFullRebalance(node: RopeNode): Boolean {
        if (node !is RopeNode.Branch) return false

        val maxHeight = max(node.left.height, node.right.height)

        val heightDiff = abs(node.left.height - node.right.height)

        return maxHeight > calculateOptimalHeight(node.charCount) + 2 || heightDiff > 3
    }

    private fun calculateOptimalHeight(size: Int) = max(2, (ln(size.toDouble()) / ln(1.618)).toInt() + 1)

    private fun forceBalance(node: RopeNode): RopeNode {
        val leaves = ArrayList<RopeNode>(128)

        collectLeaves(node, leaves)

        return buildBalancedTree(leaves)
    }

    private fun collectLeaves(node: RopeNode, acc: MutableList<RopeNode>) {
        when (node) {
            is RopeNode.Leaf -> if (node.content.isNotEmpty()) {
                acc.add(node)
            }

            is RopeNode.Branch -> {
                collectLeaves(node.left, acc)

                collectLeaves(node.right, acc)
            }

            RopeNode.Empty -> Unit
        }
    }

    private fun collectRange(node: RopeNode, start: Int, end: Int, acc: MutableList<RopeNode>) {
        if (start >= end || node === RopeNode.Empty) return

        if (start == 0 && end == node.charCount) {
            acc.add(node)

            return
        }

        when (node) {
            is RopeNode.Leaf -> {
                val sub = node.content.substring(start, end)

                acc.add(createLeaf(sub))
            }

            is RopeNode.Branch -> {
                val leftCount = node.left.charCount

                if (start < leftCount) {
                    collectRange(node.left, start, min(end, leftCount), acc)
                }

                if (end > leftCount) {
                    collectRange(node.right, max(0, start - leftCount), end - leftCount, acc)
                }
            }

            RopeNode.Empty -> Unit
        }
    }

    private fun buildBalancedTree(nodes: List<RopeNode>): RopeNode {
        if (nodes.isEmpty()) return RopeNode.Empty

        val mergedNodes = ArrayList<RopeNode>(nodes.size)

        var currentBuilder = StringBuilder()

        for (node in nodes) {
            if (node is RopeNode.Leaf) {
                if (currentBuilder.length + node.length <= 4096) {
                    currentBuilder.append(node.content)
                } else {
                    if (currentBuilder.isNotEmpty()) {
                        mergedNodes.add(createLeaf(currentBuilder.toString()))

                        currentBuilder = StringBuilder()
                    }

                    if (node.length > 4096) {
                        mergedNodes.add(node)
                    } else {
                        currentBuilder.append(node.content)
                    }
                }
            } else {
                if (currentBuilder.isNotEmpty()) {
                    mergedNodes.add(createLeaf(currentBuilder.toString()))

                    currentBuilder = StringBuilder()
                }

                mergedNodes.add(node)
            }
        }

        if (currentBuilder.isNotEmpty()) {
            mergedNodes.add(createLeaf(currentBuilder.toString()))
        }

        fun buildRecursive(start: Int, end: Int): RopeNode {
            val count = end - start

            return when (count) {
                0 -> RopeNode.Empty

                1 -> mergedNodes[start]

                else -> {
                    val mid = start + count / 2

                    val leftTree = buildRecursive(start, mid)

                    val rightTree = buildRecursive(mid, end)

                    RopeNode.Branch(leftTree, rightTree, RopeNode.Color.BLACK)
                }
            }
        }

        return buildRecursive(0, mergedNodes.size)
    }

    fun applyBatchOperations(deletions: List<Pair<Int, Int>>, insertions: List<Pair<Int, String>>): Rope {
        if (deletions.isEmpty() && insertions.isEmpty()) return this

        if (deletions.isEmpty()) return insertBatchFast(insertions)

        if (insertions.isEmpty()) return deleteBatchFast(deletions)

        val nodes = ArrayList<RopeNode>(deletions.size + insertions.size + 16)

        val allOps = ArrayList<RopeBatch>(deletions.size + insertions.size)

        allOps.addAll(deletions.map { RopeBatch.Delete(it.first, it.second) })

        allOps.addAll(insertions.map { RopeBatch.Insert(it.first, it.second) })

        allOps.sortWith(compareBy(RopeBatch::offset).thenBy { it is RopeBatch.Insert })

        var lastOpEnd = 0

        for (op in allOps) {
            if (op.offset > lastOpEnd) {
                collectRange(root, lastOpEnd, op.offset, nodes)
            }

            when (op) {
                is RopeBatch.Delete -> lastOpEnd = max(lastOpEnd, op.offset + op.length)

                is RopeBatch.Insert -> {
                    nodes.add(createLeaf(op.text))

                    lastOpEnd = max(lastOpEnd, op.offset)
                }
            }
        }

        if (lastOpEnd < totalChars) {
            collectRange(root, lastOpEnd, totalChars, nodes)
        }

        lineOffsetCache.clear()

        byteOffsetCache.clear()

        cachedTextRef = null

        cachedTextVersion.value = -1L

        return Rope(buildBalancedTree(nodes), charset, ropeNodeLeafFactory, textVersion + 1)
    }

    fun getByteOffset(charOffset: Int): Int {
        if (charOffset < 0 || charOffset > totalChars) throw IndexOutOfBoundsException()

        if (charOffset == 0) return getBOMSize(charset)

        byteOffsetCache[charOffset]?.let { return it }

        var bytes = 0

        var remaining = charOffset

        fun traverse(node: RopeNode) {
            if (remaining <= 0) return

            when (node) {
                is RopeNode.Leaf -> when {
                    remaining >= node.length -> {
                        bytes += node.byteCount

                        remaining -= node.length
                    }

                    else -> {
                        val multiplier = when (charset) {
                            StandardCharsets.UTF_32, StandardCharsets.UTF_32BE, StandardCharsets.UTF_32LE -> 4

                            StandardCharsets.UTF_16, StandardCharsets.UTF_16BE, StandardCharsets.UTF_16LE -> 2

                            else -> null
                        }

                        if (multiplier != null) {
                            bytes += remaining * multiplier

                            remaining = 0
                        } else if (charset == StandardCharsets.UTF_8) {
                            var localBytes = 0

                            var failedFastPath = false

                            for (i in 0 until remaining) {
                                val c = node.content[i]

                                if (Character.isSurrogate(c)) {
                                    failedFastPath = true

                                    break
                                }

                                val code = c.code

                                localBytes += when {
                                    code < 0x80 -> 1

                                    code < 0x800 -> 2

                                    else -> 3
                                }
                            }

                            bytes += if (failedFastPath) {
                                node.content.substring(0, remaining).toByteArray(charset).size
                            } else {
                                localBytes
                            }
                        } else {
                            bytes += node.content.substring(0, remaining).toByteArray(charset).size

                            remaining = 0
                        }
                    }
                }

                is RopeNode.Branch -> when {
                    remaining >= node.left.charCount -> {
                        bytes += node.left.byteCount

                        remaining -= node.left.charCount

                        traverse(node.right)
                    }

                    else -> traverse(node.left)
                }

                RopeNode.Empty -> Unit
            }
        }

        traverse(root)

        val finalBytes = bytes + getBOMSize(charset)

        if (byteOffsetCache.size < 1024) byteOffsetCache[charOffset] = finalBytes

        return finalBytes
    }

    fun getOffsetOfLine(lineIndex: Int): Int {
        if (lineIndex < 0 || lineIndex >= totalLines) throw IndexOutOfBoundsException()

        if (lineIndex == 0) return 0

        lineOffsetCache[lineIndex]?.let { return it }

        var chars = 0

        var targetLines = lineIndex

        fun traverse(node: RopeNode) {
            if (targetLines <= 0) return

            when (node) {
                is RopeNode.Leaf -> {
                    if (node.lineBreakCount < targetLines) {
                        chars += node.length

                        targetLines -= node.lineBreakCount
                    } else {
                        var i = 0

                        var linesFound = 0

                        var nextNewline = node.content.indexOf('\n', i)

                        while (nextNewline != -1 && linesFound < targetLines) {
                            linesFound++

                            i = nextNewline + 1

                            nextNewline = node.content.indexOf('\n', i)
                        }

                        chars += i

                        targetLines = 0
                    }
                }

                is RopeNode.Branch -> when {
                    node.left.lineBreakCount < targetLines -> {
                        chars += node.left.charCount

                        targetLines -= node.left.lineBreakCount

                        traverse(node.right)
                    }

                    else -> traverse(node.left)
                }

                RopeNode.Empty -> Unit
            }
        }

        traverse(root)

        if (lineOffsetCache.size < 1024) lineOffsetCache[lineIndex] = chars

        return chars
    }

    fun getPositionAtOffset(charOffset: Int): Pair<Int, Int> {
        if (charOffset < 0 || charOffset > totalChars) throw IndexOutOfBoundsException()

        var remainingOffset = charOffset

        var currentLine = 0

        fun find(node: RopeNode) {
            when (node) {
                is RopeNode.Leaf -> {
                    val textBefore = when {
                        remainingOffset >= node.length -> node.content

                        else -> node.content.substring(0, remainingOffset)
                    }

                    val linesInLeaf = textBefore.count { it == '\n' }

                    currentLine += linesInLeaf

                    if (linesInLeaf > 0) {
                        remainingOffset -= (textBefore.lastIndexOf('\n') + 1)
                    }
                }

                is RopeNode.Branch -> when {
                    remainingOffset >= node.left.charCount -> {
                        currentLine += node.left.lineBreakCount

                        remainingOffset -= node.left.charCount

                        find(node.right)
                    }

                    else -> find(node.left)
                }

                RopeNode.Empty -> Unit
            }
        }

        find(root)

        return currentLine to remainingOffset
    }

    fun getText(offset: Int, length: Int): String {
        if (offset < 0 || length < 0 || offset + length > totalChars) {
            throw IndexOutOfBoundsException()
        }

        if (length == 0) return ""

        if (offset == 0 && length == totalChars) {
            return getFullText()
        }

        val result = CharArray(length)

        var writePos = 0

        var remaining = length

        var currentOffset = offset

        fun traverse(node: RopeNode) {
            if (remaining <= 0) return

            when (node) {
                is RopeNode.Leaf -> {
                    val nodeContent = node.content

                    val nodeLength = node.length

                    when {
                        currentOffset < nodeLength -> {
                            val charsToCopy = min(nodeLength - currentOffset, remaining)

                            nodeContent.toCharArray(result, writePos, currentOffset, currentOffset + charsToCopy)

                            writePos += charsToCopy

                            remaining -= charsToCopy

                            if (remaining == 0) return

                            currentOffset = 0
                        }

                        else -> currentOffset -= nodeLength
                    }
                }

                is RopeNode.Branch -> {
                    val leftCount = node.left.charCount

                    when {
                        currentOffset < leftCount -> {
                            traverse(node.left)

                            if (remaining > 0) traverse(node.right)
                        }

                        else -> {
                            currentOffset -= leftCount

                            traverse(node.right)
                        }
                    }
                }

                RopeNode.Empty -> Unit
            }
        }

        traverse(root)

        return String(result, 0, length)
    }

    fun getFullText(): String {
        val cached = cachedTextRef?.get()

        if (cached != null && cachedTextVersion.value == textVersion) return cached

        val sb = StringBuilder(totalChars)

        fun appendAll(n: RopeNode) {
            when (n) {
                is RopeNode.Leaf -> sb.append(n.content)

                is RopeNode.Branch -> {
                    appendAll(n.left)

                    appendAll(n.right)
                }

                RopeNode.Empty -> Unit
            }
        }

        appendAll(root)

        val res = sb.toString()

        cachedTextRef = SoftReference(res)

        cachedTextVersion.value = textVersion

        return res
    }

    fun insert(offset: Int, text: String): Rope {
        if (offset < 0 || offset > totalChars) throw IndexOutOfBoundsException()

        if (text.isEmpty()) return this

        val newLeaf = createLeaf(text)

        if (root === RopeNode.Empty) {
            return Rope(newLeaf, charset, ropeNodeLeafFactory, textVersion + 1)
        }

        val (left, right) = splitAt(offset, root)

        val newRoot = concat(concat(left, newLeaf), right)

        val balanced = if (needsFullRebalance(newRoot)) forceBalance(newRoot) else newRoot

        lineOffsetCache.clear()

        byteOffsetCache.clear()

        return Rope(balanced, charset, ropeNodeLeafFactory, textVersion + 1)
    }

    fun delete(offset: Int, length: Int): Rope {
        if (offset < 0 || length < 0 || offset + length > totalChars) throw IndexOutOfBoundsException()

        if (length == 0) return this

        val (left, temp) = splitAt(offset, root)

        val (_, right) = splitAt(length, temp)

        val newRoot = concat(left, right)

        val balanced = if (needsFullRebalance(newRoot)) forceBalance(newRoot) else newRoot

        lineOffsetCache.clear()

        byteOffsetCache.clear()

        return Rope(balanced, charset, ropeNodeLeafFactory, textVersion + 1)
    }

    fun batch(block: RopeBuilder.() -> Unit) = RopeBuilder.Companion.build(this, block)

    fun insertBatchFast(insertions: List<Pair<Int, String>>): Rope {
        if (insertions.isEmpty()) return this

        val sortedInserts = insertions.sortedBy { it.first }

        val nodes = ArrayList<RopeNode>()

        var lastPos = 0

        for ((offset, text) in sortedInserts) {
            if (offset > lastPos) {
                collectRange(root, lastPos, offset, nodes)
            }

            nodes.add(createLeaf(text))

            lastPos = offset
        }

        if (lastPos < totalChars) {
            collectRange(root, lastPos, totalChars, nodes)
        }

        return Rope(buildBalancedTree(nodes), charset, ropeNodeLeafFactory, textVersion + 1)
    }

    fun deleteBatchFast(deletions: List<Pair<Int, Int>>): Rope {
        if (deletions.isEmpty()) return this

        val sortedDeletions = deletions.sortedBy { it.first }

        val nodes = ArrayList<RopeNode>()

        var currentPos = 0

        for ((delOffset, delLength) in sortedDeletions) {
            if (delOffset > currentPos) {
                collectRange(root, currentPos, delOffset, nodes)
            }

            currentPos = max(currentPos, delOffset + delLength)
        }

        if (currentPos < totalChars) {
            collectRange(root, currentPos, totalChars, nodes)
        }

        return Rope(buildBalancedTree(nodes), charset, ropeNodeLeafFactory, textVersion + 1)
    }

    fun rebuildWithCharset(newCharset: Charset): Rope {
        if (this.charset == newCharset) return this

        val newFactory = RopeNodeLeafFactory(enablePooling = true, charset = newCharset)

        fun rebuildNode(node: RopeNode): RopeNode = when (node) {
            is RopeNode.Leaf -> newFactory.createLeaf(node.content)

            is RopeNode.Branch -> {
                val newLeft = rebuildNode(node.left)

                val newRight = rebuildNode(node.right)

                RopeNode.Branch(newLeft, newRight, node.color)
            }

            RopeNode.Empty -> RopeNode.Empty
        }

        val newRoot = rebuildNode(root)

        return Rope(newRoot, newCharset, newFactory, textVersion)
    }
}