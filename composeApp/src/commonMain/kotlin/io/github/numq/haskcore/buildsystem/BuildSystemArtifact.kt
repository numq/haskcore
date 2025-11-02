package io.github.numq.haskcore.buildsystem

internal sealed interface BuildSystemArtifact {
    val path: String

    val name: String

    val displayName: String

    val isBuildable: Boolean
        get() = when (this) {
            is BuildComponent -> true

            is BuildProject -> packages.isNotEmpty()

            is HaskellFile -> true

            else -> false
        }

    val isExecutable: Boolean
        get() = when (this) {
            is BuildComponent.Executable -> true

            is BuildProject -> packages.any { pkg ->
                pkg.components.any { component ->
                    component.type == BuildType.EXECUTABLE
                }
            }

            is HaskellFile -> true

            is LiterateScript -> true

            else -> false
        }

    val isTestable: Boolean
        get() = when (this) {
            is BuildComponent.Test -> true

            is BuildProject -> packages.any { pkg ->
                pkg.components.any { component ->
                    component.type == BuildType.TEST
                }
            }

            else -> false
        }

    data class HaskellFile(
        override val path: String, override val name: String
    ) : BuildSystemArtifact {
        override val displayName = "$name (.hs)"
    }

    data class LiterateScript(
        override val path: String, override val name: String
    ) : BuildSystemArtifact {
        override val displayName = "$name (.lhs)"
    }

    sealed interface BuildComponent : BuildSystemArtifact {
        val packageName: String

        val type: BuildType

        data class Library(
            override val path: String,
            override val name: String,
            override val packageName: String,
            val exposedModules: List<String>
        ) : BuildComponent {
            override val displayName = "$name (library)"

            override val type = BuildType.LIBRARY
        }

        data class Executable(
            override val path: String,
            override val name: String,
            override val packageName: String,
            val mainFile: String?
        ) : BuildComponent {
            override val displayName = "$name (executable)"

            override val type = BuildType.EXECUTABLE
        }

        data class Test(
            override val path: String,
            override val name: String,
            override val packageName: String,
        ) : BuildComponent {
            override val displayName = "$name (test)"

            override val type = BuildType.TEST
        }

        data class Benchmark(
            override val path: String,
            override val name: String,
            override val packageName: String,
        ) : BuildComponent {
            override val displayName = "$name (benchmark)"

            override val type = BuildType.BENCHMARK
        }
    }

    data class BuildPackage(
        override val path: String,
        override val name: String,
        val components: List<BuildComponent>,
        val buildSystem: BuildSystem,
        val configFile: String
    ) : BuildSystemArtifact {
        override val displayName = "$name (${buildSystem.name.lowercase()})"
    }

    sealed interface BuildProject : BuildSystemArtifact {
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
}