package io.github.numq.haskcore.buildsystem.stack

import io.github.numq.haskcore.buildsystem.BuildSystemVersion

internal data class StackBuildSystemVersion(
    override val major: Int, override val minor: Int, override val patch: Int, val snapshot: String? = null
) : BuildSystemVersion() {
    companion object {
        fun fromString(versionString: String) = try {
            val parts = versionString.split("-")

            val versionParts = parts[0].split(".").map(String::toInt)

            StackBuildSystemVersion(
                major = versionParts.getOrElse(0) { 0 },
                minor = versionParts.getOrElse(1) { 0 },
                patch = versionParts.getOrElse(2) { 0 },
                snapshot = parts.getOrNull(1)
            )
        } catch (_: Throwable) {
            null
        }
    }

    override fun toString() = when (snapshot) {
        null -> "$major.$minor.$patch"

        else -> "$major.$minor.$patch-$snapshot"
    }
}