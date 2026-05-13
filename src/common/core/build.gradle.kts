plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.arrow.core)
    api(libs.koin.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}