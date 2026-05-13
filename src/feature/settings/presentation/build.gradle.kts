plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.common.presentation)
    implementation(projects.feature.settings.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}