package io.github.numq.haskcore.stack

sealed interface StackProject {
    val path: String

    val name: String

    enum class Requirement {
        STACK, RESOLVER, GHC_VERSION
    }

    data class None(override val path: String) : StackProject {
        override val name = "unknown"
    }

    data class Valid(
        override val path: String,
        override val name: String,
        val dependencies: List<String>,
        val resolver: String,
        val ghcVersion: String,
        val targets: List<String> = emptyList(),
        val flags: Map<String, Boolean> = emptyMap()
    ) : StackProject

    data class Invalid(
        override val path: String,
        override val name: String,
        val errors: List<String> = emptyList()
    ) : StackProject

    data class Incomplete(
        override val path: String,
        override val name: String,
        val missingRequirements: Set<Requirement> = emptySet()
    ) : StackProject
}