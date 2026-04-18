plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(projects.common.core)
    implementation(projects.api.lsp)
    implementation(projects.api.session)
    implementation(projects.api.syntax)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}