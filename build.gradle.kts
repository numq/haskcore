group = "io.github.numq"
version = "1.0.0"

plugins {
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinxAtomicfu) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

subprojects {
    group = rootProject.group
    version = rootProject.version
}