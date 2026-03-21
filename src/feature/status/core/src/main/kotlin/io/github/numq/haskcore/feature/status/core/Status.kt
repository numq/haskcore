package io.github.numq.haskcore.feature.status.core

data class Status(
    val pathSegments: List<String> = emptyList(),
    val ghc: StatusTool = StatusTool.Scanning,
    val cabal: StatusTool = StatusTool.Scanning,
    val stack: StatusTool = StatusTool.Scanning,
    val hls: StatusTool = StatusTool.Scanning
)