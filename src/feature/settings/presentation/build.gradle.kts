plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.koin.compose)
    implementation(compose.foundation)
    implementation(projects.core)
    implementation(projects.feature.settings.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}