plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.koin.compose)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(projects.core)
    implementation(projects.platform.dialog)
    implementation(projects.platform.font)
    implementation(projects.platform.window)
    implementation(projects.feature.welcome.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}