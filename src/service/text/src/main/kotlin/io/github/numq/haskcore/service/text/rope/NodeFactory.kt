package io.github.numq.haskcore.service.text.rope

internal interface NodeFactory {
    fun create(text: String): Node
}