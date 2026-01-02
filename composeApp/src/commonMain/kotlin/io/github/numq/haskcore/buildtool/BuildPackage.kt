package io.github.numq.haskcore.buildtool

internal data class BuildPackage(val path: String, val name: String, val targets: List<BuildTarget>)