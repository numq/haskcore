plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.atomicfu)
}

dependencies {
    implementation(compose.foundation)
    implementation(libs.koin.core)
    implementation(projects.core)
    testImplementation(compose.desktop.currentOs)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}