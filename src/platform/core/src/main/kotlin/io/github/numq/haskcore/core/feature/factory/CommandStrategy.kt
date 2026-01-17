package io.github.numq.haskcore.core.feature.factory

internal sealed interface CommandStrategy {
    data object Immediate : CommandStrategy

    sealed interface Channel : CommandStrategy {
        data object Unlimited : Channel

        data object Rendezvous : Channel

        data object Conflated : Channel

        data class Fixed(val capacity: Int) : Channel
    }

    data class Parallel(val limit: Int) : CommandStrategy
}