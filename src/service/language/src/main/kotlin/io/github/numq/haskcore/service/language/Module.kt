package io.github.numq.haskcore.service.language

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.service.language.gateway.HlsLspGateway
import io.github.numq.haskcore.service.language.gateway.LanguageClientProxy
import io.github.numq.haskcore.service.language.gateway.LspGateway
import io.github.numq.haskcore.service.language.semantic.HlsSemanticDecoder
import io.github.numq.haskcore.service.language.semantic.SemanticDecoder
import io.github.numq.haskcore.service.language.server.HaskellServerProvider
import io.github.numq.haskcore.service.language.server.ServerProvider
import org.eclipse.lsp4j.services.LanguageClient
import org.koin.dsl.bind
import org.koin.dsl.module

val languageModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner { HlsSemanticDecoder() } bind SemanticDecoder::class

        scopedOwner { (projectPath: String) ->
            HlsLspGateway(rootUri = projectPath, serverProvider = get())
        } bind LspGateway::class

        scopedOwner {
            LanguageClientProxy(onEvent = { event -> get<LspGateway>().handleEvent(event) })
        } bind LanguageClient::class

        scopedOwner { HaskellServerProvider(client = get()) } bind ServerProvider::class

        scopedOwner {
            HaskellLanguageService(lspGateway = get(), semanticDecoder = get())
        } bind LanguageService::class
    }
}