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
    implementation(projects.common.core)
    implementation(projects.common.presentation)
    implementation(projects.feature.explorer.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}