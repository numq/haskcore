plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.compose.splitpane)
    implementation(projects.common.presentation)
    implementation(projects.feature.workspace.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}