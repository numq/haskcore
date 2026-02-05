plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.koin.compose)
    implementation(projects.core)
    implementation(projects.platform.navigation)
    implementation(projects.platform.ui)
    implementation(projects.service.configuration)
    implementation(projects.service.document)
    implementation(projects.service.language)
    implementation(projects.service.project)
    implementation(projects.service.runtime)
    implementation(projects.service.session)
    implementation(projects.service.toolchain)
    implementation(projects.service.vfs)
    implementation(projects.feature.editor.core)
    implementation(projects.feature.editor.presentation)
    implementation(projects.feature.explorer.core)
    implementation(projects.feature.explorer.presentation)
    implementation(projects.feature.output.core)
    implementation(projects.feature.output.presentation)
    implementation(projects.feature.settings.core)
    implementation(projects.feature.settings.presentation)
    implementation(projects.feature.workspace.core)
    implementation(projects.feature.workspace.presentation)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "io.github.numq.haskcore.platform.application.ApplicationKt"

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