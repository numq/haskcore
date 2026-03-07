plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.koin.compose)
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(projects.core)
    implementation(projects.feature.bootstrap.core)
    implementation(projects.feature.bootstrap.presentation)
    implementation(projects.feature.editor.core)
    implementation(projects.feature.editor.presentation)
    implementation(projects.feature.execution.core)
    implementation(projects.feature.execution.presentation)
    implementation(projects.feature.explorer.core)
    implementation(projects.feature.explorer.presentation)
    implementation(projects.feature.log.core)
    implementation(projects.feature.log.presentation)
    implementation(projects.feature.navigation.core)
    implementation(projects.feature.navigation.presentation)
    implementation(projects.feature.output.core)
    implementation(projects.feature.output.presentation)
    implementation(projects.feature.settings.core)
    implementation(projects.feature.settings.presentation)
    implementation(projects.feature.shelf.core)
    implementation(projects.feature.shelf.presentation)
    implementation(projects.feature.status.core)
    implementation(projects.feature.status.presentation)
    implementation(projects.feature.welcome.core)
    implementation(projects.feature.welcome.presentation)
    implementation(projects.feature.workspace.core)
    implementation(projects.feature.workspace.presentation)
    implementation(projects.platform.dialog)
    implementation(projects.platform.font)
    implementation(projects.platform.theme)
    implementation(projects.service.clipboard)
    implementation(projects.service.configuration)
    implementation(projects.service.document)
    implementation(projects.service.journal)
    implementation(projects.service.keymap)
    implementation(projects.service.language)
    implementation(projects.service.logger)
    implementation(projects.service.project)
    implementation(projects.service.runtime)
    implementation(projects.service.session)
    implementation(projects.service.text)
    implementation(projects.service.toolchain)
    implementation(projects.service.vfs)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "io.github.numq.haskcore.application.ApplicationKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "haskcore"
            packageVersion = "1.0.0"
        }
    }
}