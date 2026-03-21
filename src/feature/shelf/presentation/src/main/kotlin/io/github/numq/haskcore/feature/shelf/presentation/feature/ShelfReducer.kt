package io.github.numq.haskcore.feature.shelf.presentation.feature

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.shelf.core.usecase.ObserveShelf
import io.github.numq.haskcore.feature.shelf.core.usecase.SaveLeftRatio
import io.github.numq.haskcore.feature.shelf.core.usecase.SaveRightRatio
import io.github.numq.haskcore.feature.shelf.core.usecase.SelectShelfTool
import kotlinx.coroutines.flow.map

internal class ShelfReducer(
    private val observeShelf: ObserveShelf,
    private val saveLeftRatio: SaveLeftRatio,
    private val saveRightRatio: SaveRightRatio,
    private val selectShelfTool: SelectShelfTool
) : Reducer<ShelfState, ShelfCommand, ShelfEvent> {
    override fun reduce(state: ShelfState, command: ShelfCommand) = when (command) {
        is ShelfCommand.HandleFailure -> transition(state).event(ShelfEvent.HandleFailure(throwable = command.throwable))

        is ShelfCommand.Initialize -> transition(state).effect(
            action(
                key = command.key, fallback = ShelfCommand::HandleFailure, block = {
                    observeShelf(input = Unit).fold(
                        ifLeft = ShelfCommand::HandleFailure, ifRight = ShelfCommand::InitializeSuccess
                    )
                })
        )

        is ShelfCommand.InitializeSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(ShelfCommand::UpdateShelf),
                fallback = ShelfCommand::HandleFailure
            )
        )

        is ShelfCommand.UpdateShelf -> transition(state.copy(shelf = command.shelf))

        is ShelfCommand.SelectShelfTool -> transition(state).effect(
            action(
                key = command.key, fallback = ShelfCommand::HandleFailure, block = {
                    selectShelfTool(input = SelectShelfTool.Input(tool = command.tool)).fold(
                        ifLeft = ShelfCommand::HandleFailure, ifRight = { ShelfCommand.SelectShelfToolSuccess })
                })
        )

        is ShelfCommand.SelectShelfToolSuccess -> transition(state)

        is ShelfCommand.SaveLeftRatio -> transition(state).effect(
            action(
                key = command.key, fallback = ShelfCommand::HandleFailure, block = {
                    saveLeftRatio(input = SaveLeftRatio.Input(ratio = command.ratio)).fold(
                        ifLeft = ShelfCommand::HandleFailure, ifRight = { ShelfCommand.SaveLeftRatioSuccess })
                })
        )

        is ShelfCommand.SaveLeftRatioSuccess -> transition(state)

        is ShelfCommand.SaveRightRatio -> transition(state).effect(
            action(
                key = command.key, fallback = ShelfCommand::HandleFailure, block = {
                    saveRightRatio(input = SaveRightRatio.Input(ratio = command.ratio)).fold(
                        ifLeft = ShelfCommand::HandleFailure, ifRight = { ShelfCommand.SaveRightRatioSuccess })
                })
        )

        is ShelfCommand.SaveRightRatioSuccess -> transition(state)
    }
}