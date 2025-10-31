package io.github.numq.haskcore.discovery

import org.koin.dsl.bind
import org.koin.dsl.module

internal val discoveryModule = module {
    single { DiscoveryService.Default() } bind DiscoveryService::class
}