package io.github.numq.haskcore.output

import io.github.numq.haskcore.datasource.BaseJsonDataSource
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

internal class OutputDataSource : BaseJsonDataSource<OutputMessage>() {
    override val dirName = Path(".haskcore", "output").absolutePathString()

    override val fileName = "${System.nanoTime()}.txt"
}