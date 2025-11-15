package io.github.numq.haskcore.buildsystem.ghc

import io.github.numq.haskcore.buildsystem.BuildSystemVersion

internal data class GhcBuildSystemVersion(
    override val major: Int, override val minor: Int, override val patch: Int, val patchLevel: Int = 0
) : BuildSystemVersion() {
    companion object {
        fun fromString(versionString: String) = try {
            val versionParts = versionString.split(".").map(String::toInt)

            GhcBuildSystemVersion(
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

    override fun compareTo(other: BuildSystemVersion) = when (other) {
        is GhcBuildSystemVersion -> compareValuesBy(
            this,
            other,
            GhcBuildSystemVersion::major,
            GhcBuildSystemVersion::minor,
            GhcBuildSystemVersion::patch,
            GhcBuildSystemVersion::patchLevel
        )

        else -> super.compareTo(other)
    }
}