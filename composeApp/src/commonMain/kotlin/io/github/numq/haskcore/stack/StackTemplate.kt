package io.github.numq.haskcore.stack

internal sealed interface StackTemplate {
    val name: String

    data object Default : StackTemplate {
        override val name = "new-template"
    }

    data class Custom(override val name: String) : StackTemplate
}