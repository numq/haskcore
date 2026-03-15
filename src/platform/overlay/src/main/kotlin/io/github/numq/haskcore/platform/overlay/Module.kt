package io.github.numq.haskcore.platform.overlay

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.platform.overlay.dialog.file.DesktopFileDialog
import io.github.numq.haskcore.platform.overlay.dialog.file.FileDialog
import org.koin.dsl.bind
import org.koin.dsl.module

val overlayModule = module {
    scope<ScopeQualifierType.Application> {
        scopedOwner { DesktopFileDialog() } bind FileDialog::class
    }
}