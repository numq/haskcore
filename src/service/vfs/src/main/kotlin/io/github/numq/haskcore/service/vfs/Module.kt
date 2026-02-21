package io.github.numq.haskcore.service.vfs

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val vfsModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { LocalVfsDataSource() } bind VfsDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            CachedVfsService(scope = scope, vfsDataSource = get())
        } bind VfsService::class
    }
}