package io.github.numq.haskcore.cabal

import io.github.numq.haskcore.toolchain.ToolchainVersion

internal data class CabalVersion(
    override val major: Int, override val minor: Int, override val patch: Int, val revision: Int = 0
) : ToolchainVersion() {
    companion object {
        fun fromString(versionString: String) = try {
            val parts = versionString.split("-r")

            val versionParts = parts[0].split(".").map(String::toInt)

            CabalVersion(
                major = versionParts.getOrElse(0) { 0 },
                minor = versionParts.getOrElse(1) { 0 },
                patch = versionParts.getOrElse(2) { 0 },
                revision = parts.getOrNull(1)?.toInt() ?: 0
            )
        } catch (_: Throwable) {
            null
        }
    }

    override fun toString() = when {
        revision > 0 -> "$major.$minor.$patch.$revision"

        else -> "$major.$minor.$patch"
    }

    override fun compareTo(other: ToolchainVersion) = when (other) {
        is CabalVersion -> compareValuesBy(
            this,
            other,
            CabalVersion::major,
            CabalVersion::minor,
            CabalVersion::patch,
            CabalVersion::revision
        )

        else -> super.compareTo(other)
    }
}