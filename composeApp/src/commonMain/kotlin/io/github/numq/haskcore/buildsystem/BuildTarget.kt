package io.github.numq.haskcore.buildsystem

internal sealed interface BuildTarget {
    val path: String

    val name: String

    val displayName: String

    val isBuildable: Boolean
        get() = when (this) {
            is BuildProject -> true

            is HaskellFile -> true

            else -> false
        }

    val isExecutable: Boolean
        get() = when (this) {
            is BuildProject -> packages.any(BuildPackage::hasExecutableComponents)

            is HaskellFile -> true

            is LiterateScript -> true
        }

    val isTestable: Boolean
        get() = when (this) {
            is BuildProject -> packages.any(BuildPackage::hasTestComponents)

            else -> false
        }

    sealed interface BuildProject : BuildTarget {
        val packages: List<BuildPackage>

        val buildSystem: BuildSystem

        data class Stack(
            override val path: String,
            override val name: String,
            override val packages: List<BuildPackage>,
            val resolver: String,
            val ghcVersion: String?
        ) : BuildProject {
            override val displayName = "$name (stack)"

            override val buildSystem = BuildSystem.STACK
        }

        data class Cabal(
            override val path: String, override val name: String, override val packages: List<BuildPackage>
        ) : BuildProject {
            override val displayName = "$name (cabal)"

            override val buildSystem = BuildSystem.CABAL
        }
    }

    data class HaskellFile(
        override val path: String, override val name: String
    ) : BuildTarget {
        override val displayName = "$name (.hs)"
    }

    data class LiterateScript(
        override val path: String, override val name: String
    ) : BuildTarget {
        override val displayName = "$name (.lhs)"
    }
}