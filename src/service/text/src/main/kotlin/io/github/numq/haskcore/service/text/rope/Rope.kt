package io.github.numq.haskcore.service.text.rope

import io.github.numq.haskcore.common.core.text.TextEncoding
import kotlinx.atomicfu.atomic
import java.lang.ref.SoftReference
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

internal class Rope private constructor(
    private val root: Node,
    private val encoding: TextEncoding,
    private val ropeNodeFactory: RopeNodeFactory,
    val textVersion: Long = 0L,
) {
    constructor(
        initialText: String,
        encoding: TextEncoding,
        ropeNodeFactory: RopeNodeFactory,
    ) : this(
        root = when {
            initialText.isEmpty() -> Node.Empty

            else -> ropeNodeFactory.create(text = initialText)
        }, encoding = encoding, ropeNodeFactory = ropeNodeFactory, textVersion = 0L
    )

    private val lineOffsetCache = Collections.synchronizedMap(object : LinkedHashMap<Int, Int>(512, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Int>) = size > 1024
    })

    private val byteOffsetCache = Collections.synchronizedMap(object : LinkedHashMap<Int, Int>(512, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Int>) = size > 1024
    })

    private var cachedTextRef: SoftReference<String>? = null

    private val cachedTextVersion = atomic(-1L)

    val totalBytes: Int get() = root.byteCount + encoding.bomSize

    val totalChars: Int get() = root.charCount

    val totalLines: Int get() = root.lineBreakCount + 1

    val maxLineLength: Int get() = root.maxLineLength

    private fun createNode(text: String) = ropeNodeFactory.create(text = text)

    private fun isRed(node: Node) = node.color == Node.Color.RED

    private fun rotateLeft(node: Node.Branch): Node.Branch {
        val x = node.right

        if (x !is Node.Branch) return node

        return Node.Branch(
            left = Node.Branch(node.left, x.left, node.color), right = x.right, color = x.color
        )
    }

    private fun rotateRight(node: Node.Branch): Node.Branch {
        val x = node.left

        if (x !is Node.Branch) return node

        return Node.Branch(
            left = x.left, right = Node.Branch(x.right, node.right, node.color), color = x.color
        )
    }

    private fun flipColors(node: Node.Branch): Node.Branch {
        fun flip(color: Node.Color) = when (color) {
            Node.Color.BLACK -> Node.Color.RED

            else -> Node.Color.BLACK
        }

        val newLeft = when (val left = node.left) {
            is Node.Leaf -> left.copy(color = flip(left.color))

            is Node.Branch -> left.copy(color = flip(left.color))

            Node.Empty -> left
        }

        val newRight = when (val right = node.right) {
            is Node.Leaf -> right.copy(color = flip(right.color))

            is Node.Branch -> right.copy(color = flip(right.color))

            Node.Empty -> right
        }

        return node.copy(left = newLeft, right = newRight, color = flip(node.color))
    }

    private fun quickBalance(node: Node): Node {
        if (node !is Node.Branch) return node

        var current = node

        if (isRed(current.right) && !isRed(current.left) && current.right is Node.Branch) {
            current = rotateLeft(current)
        }

        if (isRed(current.left) && current.left is Node.Branch && isRed(current.left.left)) {
            current = rotateRight(current)
        }

        if (isRed(current.left) && isRed(current.right)) {
            current = flipColors(current)
        }

        return current
    }

    private fun splitAt(offset: Int, node: Node): Pair<Node, Node> = when {
        offset <= 0 -> Pair(Node.Empty, node)

        offset >= node.charCount -> Pair(node, Node.Empty)

        node is Node.Leaf -> {
            val leftContent = node.text.substring(0, offset)

            val rightContent = node.text.substring(offset)

            Pair(createNode(leftContent), createNode(rightContent))
        }

        node is Node.Branch -> when {
            offset < node.left.charCount -> {
                val (leftLeft, leftRight) = splitAt(offset, node.left)

                Pair(leftLeft, concat(leftRight, node.right))
            }

            else -> {
                val (rightLeft, rightRight) = splitAt(offset - node.left.charCount, node.right)

                Pair(concat(node.left, rightLeft), rightRight)
            }
        }

        else -> Pair(Node.Empty, Node.Empty)
    }

    private fun concat(left: Node, right: Node): Node {
        if (left === Node.Empty) return right

        if (right === Node.Empty) return left

        if (left is Node.Leaf && right is Node.Leaf && left.length + right.length <= 8192) {
            return createNode(left.text + right.text)
        }

        return quickBalance(Node.Branch(left, right, Node.Color.RED))
    }

    private fun needsFullRebalance(node: Node): Boolean {
        if (node !is Node.Branch) return false

        val maxHeight = max(node.left.height, node.right.height)

        val heightDiff = abs(node.left.height - node.right.height)

        return maxHeight > calculateOptimalHeight(node.charCount) + 2 || heightDiff > 3
    }

    private fun calculateOptimalHeight(size: Int) = max(2, (ln(size.toDouble()) / ln(1.618)).toInt() + 1)

    private fun forceBalance(node: Node): Node {
        val leaves = ArrayList<Node>(128)

        collectLeaves(node, leaves)

        return buildBalancedTree(leaves)
    }

    private fun collectLeaves(node: Node, acc: MutableList<Node>) {
        when (node) {
            is Node.Leaf -> if (node.text.isNotEmpty()) {
                acc.add(node)
            }

            is Node.Branch -> {
                collectLeaves(node.left, acc)

                collectLeaves(node.right, acc)
            }

            Node.Empty -> Unit
        }
    }

    private fun collectRange(node: Node, start: Int, end: Int, acc: MutableList<Node>) {
        if (start >= end || node === Node.Empty) return

        if (start == 0 && end == node.charCount) {
            acc.add(node)

            return
        }

        when (node) {
            is Node.Leaf -> {
                val sub = node.text.substring(start, end)

                acc.add(createNode(sub))
            }

            is Node.Branch -> {
                val leftCount = node.left.charCount

                if (start < leftCount) {
                    collectRange(node.left, start, min(end, leftCount), acc)
                }

                if (end > leftCount) {
                    collectRange(node.right, max(0, start - leftCount), end - leftCount, acc)
                }
            }

            Node.Empty -> Unit
        }
    }

    private fun buildBalancedTree(nodes: List<Node>): Node {
        if (nodes.isEmpty()) return Node.Empty

        val mergedNodes = ArrayList<Node>(nodes.size)

        var currentBuilder = StringBuilder()

        for (node in nodes) {
            if (node is Node.Leaf) {
                if (currentBuilder.length + node.length <= 4096) {
                    currentBuilder.append(node.text)
                } else {
                    if (currentBuilder.isNotEmpty()) {
                        mergedNodes.add(createNode(currentBuilder.toString()))

                        currentBuilder = StringBuilder()
                    }

                    if (node.length > 4096) {
                        mergedNodes.add(node)
                    } else {
                        currentBuilder.append(node.text)
                    }
                }
            } else {
                if (currentBuilder.isNotEmpty()) {
                    mergedNodes.add(createNode(currentBuilder.toString()))

                    currentBuilder = StringBuilder()
                }

                mergedNodes.add(node)
            }
        }

        if (currentBuilder.isNotEmpty()) {
            mergedNodes.add(createNode(currentBuilder.toString()))
        }

        fun buildRecursive(start: Int, end: Int): Node {
            val count = end - start

            return when (count) {
                0 -> Node.Empty

                1 -> mergedNodes[start]

                else -> {
                    val mid = start + count / 2

                    val leftTree = buildRecursive(start, mid)

                    val rightTree = buildRecursive(mid, end)

                    Node.Branch(leftTree, rightTree, Node.Color.BLACK)
                }
            }
        }

        return buildRecursive(0, mergedNodes.size)
    }

    fun applyBatchOperations(deletions: List<Pair<Int, Int>>, insertions: List<Pair<Int, String>>): Rope {
        if (deletions.isEmpty() && insertions.isEmpty()) return this

        if (deletions.isEmpty()) return insertBatchFast(insertions)

        if (insertions.isEmpty()) return deleteBatchFast(deletions)

        val nodes = ArrayList<Node>(deletions.size + insertions.size + 16)

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
                    nodes.add(createNode(op.text))

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

        return Rope(buildBalancedTree(nodes), encoding, ropeNodeFactory, textVersion + 1)
    }

    fun getByteOffset(charOffset: Int): Int {
        if (charOffset < 0 || charOffset > totalChars) throw IndexOutOfBoundsException()

        if (charOffset == 0) return encoding.bomSize

        byteOffsetCache[charOffset]?.let { return it }

        var bytes = 0

        var remaining = charOffset

        fun traverse(node: Node) {
            if (remaining <= 0) return

            when (node) {
                is Node.Leaf -> when {
                    remaining >= node.length -> {
                        bytes += node.byteCount

                        remaining -= node.length
                    }

                    else -> {
                        val charset = encoding.charset

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
                                val c = node.text[i]

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
                                node.text.substring(0, remaining).toByteArray(charset).size
                            } else {
                                localBytes
                            }
                        } else {
                            bytes += node.text.substring(0, remaining).toByteArray(charset).size

                            remaining = 0
                        }
                    }
                }

                is Node.Branch -> when {
                    remaining >= node.left.charCount -> {
                        bytes += node.left.byteCount

                        remaining -= node.left.charCount

                        traverse(node.right)
                    }

                    else -> traverse(node.left)
                }

                Node.Empty -> Unit
            }
        }

        traverse(root)

        val finalBytes = bytes + encoding.bomSize

        if (byteOffsetCache.size < 1024) byteOffsetCache[charOffset] = finalBytes

        return finalBytes
    }

    fun getOffsetOfLine(lineIndex: Int): Int {
        if (lineIndex < 0 || lineIndex >= totalLines) throw IndexOutOfBoundsException()

        if (lineIndex == 0) return 0

        lineOffsetCache[lineIndex]?.let { return it }

        var chars = 0

        var targetLines = lineIndex

        fun traverse(node: Node) {
            if (targetLines <= 0) return

            when (node) {
                is Node.Leaf -> {
                    if (node.lineBreakCount < targetLines) {
                        chars += node.length

                        targetLines -= node.lineBreakCount
                    } else {
                        var i = 0

                        var linesFound = 0

                        var nextNewline = node.text.indexOf('\n', i)

                        while (nextNewline != -1 && linesFound < targetLines) {
                            linesFound++

                            i = nextNewline + 1

                            nextNewline = node.text.indexOf('\n', i)
                        }

                        chars += i

                        targetLines = 0
                    }
                }

                is Node.Branch -> when {
                    node.left.lineBreakCount < targetLines -> {
                        chars += node.left.charCount

                        targetLines -= node.left.lineBreakCount

                        traverse(node.right)
                    }

                    else -> traverse(node.left)
                }

                Node.Empty -> Unit
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

        fun find(node: Node) {
            when (node) {
                is Node.Leaf -> {
                    val textBefore = when {
                        remainingOffset >= node.length -> node.text

                        else -> node.text.substring(0, remainingOffset)
                    }

                    val linesInLeaf = textBefore.count { it == '\n' }

                    currentLine += linesInLeaf

                    if (linesInLeaf > 0) {
                        remainingOffset -= (textBefore.lastIndexOf('\n') + 1)
                    }
                }

                is Node.Branch -> when {
                    remainingOffset >= node.left.charCount -> {
                        currentLine += node.left.lineBreakCount

                        remainingOffset -= node.left.charCount

                        find(node.right)
                    }

                    else -> find(node.left)
                }

                Node.Empty -> Unit
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

        fun traverse(node: Node) {
            if (remaining <= 0) return

            when (node) {
                is Node.Leaf -> {
                    val nodeContent = node.text

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

                is Node.Branch -> {
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

                Node.Empty -> Unit
            }
        }

        traverse(root)

        return String(result, 0, length)
    }

    fun getFullText(): String {
        val cached = cachedTextRef?.get()

        if (cached != null && cachedTextVersion.value == textVersion) return cached

        val sb = StringBuilder(totalChars)

        fun appendAll(n: Node) {
            when (n) {
                is Node.Leaf -> sb.append(n.text)

                is Node.Branch -> {
                    appendAll(n.left)

                    appendAll(n.right)
                }

                Node.Empty -> Unit
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

        val newLeaf = createNode(text)

        if (root === Node.Empty) {
            return Rope(
                root = newLeaf, encoding = encoding, ropeNodeFactory = ropeNodeFactory, textVersion = textVersion + 1
            )
        }

        val (left, right) = splitAt(offset, root)

        val newRoot = concat(concat(left, newLeaf), right)

        val balanced = if (needsFullRebalance(newRoot)) forceBalance(newRoot) else newRoot

        lineOffsetCache.clear()

        byteOffsetCache.clear()

        return Rope(
            root = balanced, encoding = encoding, ropeNodeFactory = ropeNodeFactory, textVersion = textVersion + 1
        )
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

        return Rope(
            root = balanced, encoding = encoding, ropeNodeFactory = ropeNodeFactory, textVersion = textVersion + 1
        )
    }

    fun batch(block: RopeBuilder.() -> Unit) = RopeBuilder.Companion.build(this, block)

    fun insertBatchFast(insertions: List<Pair<Int, String>>): Rope {
        if (insertions.isEmpty()) return this

        val sortedInserts = insertions.sortedBy(Pair<Int, String>::first)

        val nodes = ArrayList<Node>()

        var lastPos = 0

        for ((offset, text) in sortedInserts) {
            if (offset > lastPos) {
                collectRange(node = root, start = lastPos, end = offset, acc = nodes)
            }

            nodes.add(createNode(text = text))

            lastPos = offset
        }

        if (lastPos < totalChars) {
            collectRange(node = root, start = lastPos, end = totalChars, acc = nodes)
        }

        return Rope(
            root = buildBalancedTree(nodes),
            encoding = encoding,
            ropeNodeFactory = ropeNodeFactory,
            textVersion = textVersion + 1
        )
    }

    fun deleteBatchFast(deletions: List<Pair<Int, Int>>): Rope {
        if (deletions.isEmpty()) return this

        val sortedDeletions = deletions.sortedBy(Pair<Int, Int>::first)

        val nodes = ArrayList<Node>()

        var currentPos = 0

        for ((delOffset, delLength) in sortedDeletions) {
            if (delOffset > currentPos) {
                collectRange(node = root, start = currentPos, end = delOffset, acc = nodes)
            }

            currentPos = max(currentPos, delOffset + delLength)
        }

        if (currentPos < totalChars) {
            collectRange(node = root, start = currentPos, end = totalChars, acc = nodes)
        }

        return Rope(
            root = buildBalancedTree(nodes),
            encoding = encoding,
            ropeNodeFactory = ropeNodeFactory,
            textVersion = textVersion + 1
        )
    }

    fun rebuildWithEncoding(newEncoding: TextEncoding): Rope {
        if (this.encoding == newEncoding) return this

        val newFactory = RopeNodeFactory(enablePooling = true, encoding = newEncoding)

        fun rebuildNode(node: Node): Node = when (node) {
            is Node.Leaf -> newFactory.create(text = node.text)

            is Node.Branch -> {
                val newLeft = rebuildNode(node = node.left)

                val newRight = rebuildNode(node = node.right)

                Node.Branch(newLeft, newRight, color = node.color)
            }

            Node.Empty -> Node.Empty
        }

        val newRoot = rebuildNode(root)

        return Rope(newRoot, newEncoding, newFactory, textVersion)
    }
}