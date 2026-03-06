package io.github.numq.haskcore.service.text.buffer.rope

internal class RopeBuilder private constructor() {
    companion object {
        fun build(baseRope: Rope, builder: RopeBuilder.() -> Unit): Rope {
            val instance = RopeBuilder()

            instance.builder()

            return instance.buildInternal(baseRope)
        }
    }

    private val insertOperations = ArrayList<Pair<Int, String>>()

    private val deleteOperations = ArrayList<Pair<Int, Int>>()

    @Synchronized
    fun insert(offset: Int, text: String): RopeBuilder {
        if (text.isNotEmpty()) insertOperations.add(offset to text)

        return this
    }

    @Synchronized
    fun delete(offset: Int, length: Int): RopeBuilder {
        if (length > 0) deleteOperations.add(offset to length)

        return this
    }

    @Synchronized
    private fun buildInternal(baseRope: Rope): Rope {
        if (insertOperations.isEmpty() && deleteOperations.isEmpty()) return baseRope

        return baseRope.applyBatchOperations(deleteOperations, insertOperations)
    }

    @Synchronized
    fun build(baseRope: Rope) = buildInternal(baseRope)

    @Synchronized
    fun clear(): RopeBuilder {
        insertOperations.clear()

        deleteOperations.clear()

        return this
    }
}