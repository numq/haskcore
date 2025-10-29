package io.github.numq.haskcore.explorer

import io.github.numq.haskcore.datasource.BaseJsonDataSource

internal class ExplorerDataSource : BaseJsonDataSource<Explorer>() {
    override val dirName = ".haskcore"

    override val fileName = "explorer.json"
}