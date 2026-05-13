plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.atomicfu)
}

dependencies {
    implementation(projects.common.presentation)
    implementation(projects.feature.editor.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}