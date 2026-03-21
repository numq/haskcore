plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.atomicfu)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(libs.gson)
    implementation(libs.lsp4j)
    implementation(libs.lsp4j.jsonrpc)
    implementation(projects.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}