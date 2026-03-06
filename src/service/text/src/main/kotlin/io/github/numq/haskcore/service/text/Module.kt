package io.github.numq.haskcore.service.text

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.service.text.buffer.RopeTextBufferFactory
import io.github.numq.haskcore.service.text.buffer.TextBufferFactory
import io.github.numq.haskcore.service.text.occurrence.HaskellOccurrenceProvider
import io.github.numq.haskcore.service.text.occurrence.OccurrenceProvider
import io.github.numq.haskcore.service.text.syntax.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import org.treesitter.TreeSitterHaskell

val textModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner {
            HaskellQueryProvider(
                language = TreeSitterHaskell(),
                highlightsURL = javaClass.getResource("/queries/highlights.scm"),
                localsURL = javaClass.getResource("/queries/locals.scm"),
                injectionsURL = javaClass.getResource("/queries/injections.scm")
            )
        } binds arrayOf(QueryProvider::class)

        scopedOwner { HaskellSyntaxEngine(haskell = get<QueryProvider>().language) } bind SyntaxEngine::class

        scopedOwner { DefaultTextServiceInitializer(queryProvider = get()) } bind TextServiceInitializer::class
    }

    scope<ScopeQualifier.Document> {
        scopedOwner { RopeTextBufferFactory() } bind TextBufferFactory::class

        scopedOwner {
            HaskellScopeProvider(queryProvider = get<QueryProvider>(), syntaxEngine = get())
        } bind ScopeProvider::class

        scopedOwner {
            HaskellSyntaxTokenProvider(queryProvider = get<QueryProvider>(), syntaxEngine = get())
        } bind SyntaxTokenProvider::class

        scopedOwner {
            HaskellOccurrenceProvider(syntaxEngine = get(), queryProvider = get<QueryProvider>())
        } bind OccurrenceProvider::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalTextService(
                scope = scope,
                syntaxEngine = get(),
                occurrenceProvider = get(),
                scopeProvider = get(),
                syntaxTokenProvider = get(),
                bufferFactory = get()
            )
        } bind TextService::class
    }
}