plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}