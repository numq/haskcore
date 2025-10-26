package io.github.numq.haskcore.output

import io.github.numq.haskcore.output.usecase.ClearOutput
import io.github.numq.haskcore.output.usecase.ExportOutput
import io.github.numq.haskcore.output.usecase.GetOutputExportPath
import io.github.numq.haskcore.output.usecase.ObserveOutputMessages
import org.koin.dsl.bind
import org.koin.dsl.module

internal val outputModule = module {
    single { OutputDataSource() }

    single { OutputRepository.Default(outputDataSource = get()) } bind OutputRepository::class

    single { ClearOutput(outputRepository = get()) }

    single { ExportOutput(outputRepository = get()) }

    single { GetOutputExportPath(outputRepository = get()) }

    single { ObserveOutputMessages(outputRepository = get()) }
}