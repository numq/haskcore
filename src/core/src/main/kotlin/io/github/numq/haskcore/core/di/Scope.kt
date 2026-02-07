package io.github.numq.haskcore.core.di

import org.koin.core.definition.KoinDefinition
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.dsl.ScopeDSL
import org.koin.dsl.onClose

inline fun <reified T> ScopeDSL.scopedOwner(
    qualifier: Qualifier? = null, noinline definition: Scope.(ParametersHolder) -> T
): KoinDefinition<T> = scoped(qualifier = qualifier, definition = definition) onClose { instance ->
    (instance as? AutoCloseable)?.close()
}