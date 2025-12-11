package io.github.numq.haskcore.toolchain

internal abstract class ToolchainVersion {
    protected abstract val major: Int

    protected abstract val minor: Int

    protected abstract val patch: Int

    init {
        require(major >= 0) { "Major version must be non-negative" }

        require(minor >= 0) { "Minor version must be non-negative" }

        require(patch >= 0) { "Patch version must be non-negative" }
    }

    override fun toString() = "$major.$minor.$patch"

    open fun compareTo(other: ToolchainVersion): Int {
        require(this::class == other::class) { "Cannot compare different version types" }

        return when {
            major != other.major -> major.compareTo(other.major)

            minor != other.minor -> minor.compareTo(other.minor)

            else -> patch.compareTo(other.patch)
        }
    }
}