plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.atomicfu)
}

dependencies {
    implementation(libs.gson)
    implementation(libs.lsp4j)
    implementation(libs.lsp4j.jsonrpc)
    implementation(projects.common.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}