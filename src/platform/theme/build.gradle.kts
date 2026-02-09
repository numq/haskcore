plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(compose.foundation)
    implementation(compose.material3)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}