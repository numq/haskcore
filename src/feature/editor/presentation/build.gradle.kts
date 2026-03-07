plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.atomicfu)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.koin.compose)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(projects.core)
    implementation(projects.platform.font)
    implementation(projects.platform.theme)
    implementation(projects.feature.editor.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}