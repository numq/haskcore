package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.cabal.CabalBuildSystemVersion
import io.github.numq.haskcore.buildsystem.ghc.GhcBuildSystemVersion
import io.github.numq.haskcore.buildsystem.stack.StackBuildSystemVersion

internal data class BuildSystemVersions(
    val stackVersion: StackBuildSystemVersion,
    val cabalVersion: CabalBuildSystemVersion,
    val ghcVersion: GhcBuildSystemVersion
)