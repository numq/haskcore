package io.github.numq.haskcore.buildsystem.cabal

import io.github.numq.haskcore.buildsystem.BuildSystemVersion

internal data class CabalBuildSystemVersion(
    override val major: Int, override val minor: Int, override val patch: Int, val revision: Int = 0
) : BuildSystemVersion() {
    companion object {
        fun fromString(versionString: String) = try {
            val parts = versionString.split("-r")

            val versionParts = parts[0].split(".").map(String::toInt)

            CabalBuildSystemVersion(
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

    override fun compareTo(other: BuildSystemVersion) = when (other) {
        is CabalBuildSystemVersion -> compareValuesBy(
            this,
            other,
            CabalBuildSystemVersion::major,
            CabalBuildSystemVersion::minor,
            CabalBuildSystemVersion::patch,
            CabalBuildSystemVersion::revision
        )

        else -> super.compareTo(other)
    }
}