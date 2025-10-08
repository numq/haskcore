import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinxAtomicfu)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.kotlinx.immutable)
            implementation(libs.kotlinx.serializationJson)

            // todo
            implementation("io.methvin:directory-watcher:0.19.1")

            implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0")
            implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.24.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            implementation("org.openjfx:javafx-base:24:win")
            implementation("org.openjfx:javafx-graphics:24:win")
            implementation("org.openjfx:javafx-controls:24:win")
            implementation("org.openjfx:javafx-swing:24:win")

            implementation("org.fxmisc.richtext:richtextfx:0.11.6")
            implementation("org.fxmisc.flowless:flowless:0.7.4")
            implementation("org.fxmisc.undo:undofx:2.1.1")
            implementation("org.fxmisc.wellbehaved:wellbehavedfx:0.3.3")
            implementation("org.fxmisc.easybind:easybind:1.0.3")
        }

        jvmTest.dependencies {
            implementation(libs.junit)
            implementation(libs.mockk)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "io.github.numq.haskcore.application.ApplicationKt"
//        mainClass = "io.github.numq.haskcore.sandbox.SandboxApplicationKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "haskcore"
            packageVersion = "1.0.0"
        }
    }
}