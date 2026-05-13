plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(projects.entrypoint)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "io.github.numq.haskcore.platform.MainKt"

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