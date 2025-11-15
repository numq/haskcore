package io.github.numq.haskcore.buildsystem

internal abstract class BuildSystemVersion {
    abstract val major: Int

    abstract val minor: Int

    abstract val patch: Int

    init {
        require(major >= 0) { "Major version must be non-negative" }

        require(minor >= 0) { "Minor version must be non-negative" }

        require(patch >= 0) { "Patch version must be non-negative" }
    }

    override fun toString() = "$major.$minor.$patch"

    open fun compareTo(other: BuildSystemVersion): Int {
        require(this::class == other::class) { "Cannot compare different version types" }

        return when {
            major != other.major -> major.compareTo(other.major)

            minor != other.minor -> minor.compareTo(other.minor)

            else -> patch.compareTo(other.patch)
        }
    }
}