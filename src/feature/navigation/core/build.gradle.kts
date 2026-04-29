plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(projects.common.core)
    implementation(projects.api.session)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}