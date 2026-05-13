plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.atomicfu)
}

dependencies {
    api(libs.compose.foundation)
    api(libs.compose.material3)
    api(libs.compose.material.icons.extended)
    api(libs.compose.runtime)
    api(libs.compose.ui)
    api(libs.koin.compose)
    api(projects.common.core)
    testImplementation(libs.compose.desktop)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}