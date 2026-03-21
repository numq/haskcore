package io.github.numq.haskcore.service.syntax

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.service.syntax.folding.SyntaxFoldingProvider
import io.github.numq.haskcore.service.syntax.folding.HaskellSyntaxFoldingProvider
import io.github.numq.haskcore.service.syntax.initializer.DefaultSyntaxServiceInitializer
import io.github.numq.haskcore.service.syntax.initializer.SyntaxServiceInitializer
import io.github.numq.haskcore.service.syntax.occurrence.HaskellSyntaxOccurrenceProvider
import io.github.numq.haskcore.service.syntax.occurrence.SyntaxOccurrenceProvider
import io.github.numq.haskcore.service.syntax.query.HaskellSyntaxQueryProvider
import io.github.numq.haskcore.service.syntax.query.SyntaxQueryProvider
import io.github.numq.haskcore.service.syntax.symbol.DefaultSymbolTable
import io.github.numq.haskcore.service.syntax.symbol.HaskellSymbolIndexer
import io.github.numq.haskcore.service.syntax.symbol.SymbolIndexer
import io.github.numq.haskcore.service.syntax.symbol.SymbolTable
import io.github.numq.haskcore.service.syntax.token.HaskellSyntaxTokenProvider
import io.github.numq.haskcore.service.syntax.token.SyntaxTokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import org.treesitter.TreeSitterHaskell

val syntaxModule = module {
    scope<ScopeQualifierType.Application> {
        scopedOwner {
            HaskellSyntaxQueryProvider(
                language = TreeSitterHaskell(),
                highlightsURL = javaClass.getResource("/queries/highlights.scm"),
                localsURL = javaClass.getResource("/queries/locals.scm"),
                injectionsURL = javaClass.getResource("/queries/injections.scm")
            )
        } binds arrayOf(SyntaxQueryProvider::class)


        scopedOwner { DefaultSyntaxServiceInitializer(queryProvider = get()) } bind SyntaxServiceInitializer::class
    }

    scope<ScopeQualifierType.Document> {
        scopedOwner { HaskellSyntaxFoldingProvider() } bind SyntaxFoldingProvider::class

        scopedOwner { DefaultSymbolTable() } bind SymbolTable::class

        scopedOwner { HaskellSymbolIndexer(symbolTable = get()) } bind SymbolIndexer::class

        scopedOwner { HaskellSyntaxOccurrenceProvider(symbolTable = get()) } bind SyntaxOccurrenceProvider::class

        scopedOwner { HaskellSyntaxTokenProvider() } bind SyntaxTokenProvider::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            HaskellSyntaxService(
                scope = scope,
                foldingProvider = get(),
                occurrenceProvider = get(),
                symbolIndexer = get(),
                syntaxTokenProvider = get(),
                queryProvider = get()
            )
        } bind SyntaxService::class
    }
}