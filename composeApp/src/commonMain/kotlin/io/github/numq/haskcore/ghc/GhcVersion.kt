package io.github.numq.haskcore.ghc

import io.github.numq.haskcore.toolchain.ToolchainVersion

internal data class GhcVersion(
    override val major: Int, override val minor: Int, override val patch: Int, val patchLevel: Int = 0
) : ToolchainVersion() {
    companion object {
        fun fromString(versionString: String) = try {
            val versionParts = versionString.split(".").map(String::toInt)

            GhcVersion(
                major = versionParts.getOrElse(0) { 0 },
                minor = versionParts.getOrElse(1) { 0 },
                patch = versionParts.getOrElse(2) { 0 },
                patchLevel = versionParts.getOrElse(3) { 0 })
        } catch (_: Throwable) {
            null
        }
    }

    override fun toString() = when {
        patchLevel > 0 -> "$major.$minor.$patch.$patchLevel"

        else -> "$major.$minor.$patch"
    }

    override fun compareTo(other: ToolchainVersion) = when (other) {
        is GhcVersion -> compareValuesBy(
            this,
            other,
            GhcVersion::major,
            GhcVersion::minor,
            GhcVersion::patch,
            GhcVersion::patchLevel
        )

        else -> super.compareTo(other)
    }
}