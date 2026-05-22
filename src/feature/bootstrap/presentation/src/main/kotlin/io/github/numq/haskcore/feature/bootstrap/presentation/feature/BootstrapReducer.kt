package io.github.numq.haskcore.feature.bootstrap.presentation.feature

import arrow.core.getOrElse
import io.github.numq.haskcore.common.presentation.feature.Reducer
import io.github.numq.haskcore.common.presentation.feature.action
import io.github.numq.haskcore.common.presentation.feature.effect
import io.github.numq.haskcore.common.presentation.feature.event
import io.github.numq.haskcore.common.presentation.font.FontManager
import io.github.numq.haskcore.common.presentation.font.FontResources
import io.github.numq.haskcore.feature.bootstrap.core.usecase.Boot
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class BootstrapReducer(
    private val fontManager: FontManager,
    private val boot: Boot,
) : Reducer<BootstrapState, BootstrapCommand, BootstrapEvent> {
    override fun reduce(state: BootstrapState, command: BootstrapCommand) = when (command) {
        is BootstrapCommand.HandleFailure -> transition(state).event(BootstrapEvent.HandleFailure(throwable = command.throwable))

        is BootstrapCommand.Initialize -> transition(state).effect(
            action(key = command.key, fallback = BootstrapCommand::HandleFailure, block = {
                coroutineScope {
                    val bootstrapDeferred = async {
                        boot(input = Unit).getOrElse { throwable ->
                            throw throwable
                        }
                    }

                    val welcomeLogoFontDeferred = async {
                        fontManager.loadFont(fileName = FontResources.LOGO, size = 96f).getOrElse { throwable ->
                            throw throwable
                        }
                    }

                    val welcomeMonoFontDeferred = async {
                        fontManager.loadFont(fileName = FontResources.MONO, size = 32f).getOrElse { throwable ->
                            throw throwable
                        }
                    }

                    val editorMonoFontDeferred = async {
                        fontManager.loadFont(fileName = FontResources.MONO).getOrElse { throwable ->
                            throw throwable
                        }
                    }

                    BootstrapCommand.InitializeSuccess(
                        bootstrap = bootstrapDeferred.await(),
                        welcomeLogoFont = welcomeLogoFontDeferred.await(),
                        welcomeMonoFont = welcomeMonoFontDeferred.await(),
                        editorMonoFont = editorMonoFontDeferred.await()
                    )
                }
            })
        )

        is BootstrapCommand.InitializeSuccess -> with(command) {
            transition(
                BootstrapState.Content(
                    bootstrap = bootstrap,
                    welcomeLogoFont = welcomeLogoFont,
                    welcomeMonoFont = welcomeMonoFont,
                    editorMonoFont = editorMonoFont
                )
            )
        }
    }
}