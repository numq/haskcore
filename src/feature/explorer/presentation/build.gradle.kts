plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.koin.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(projects.common.core)
    implementation(projects.common.presentation)
    implementation(projects.feature.explorer.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}