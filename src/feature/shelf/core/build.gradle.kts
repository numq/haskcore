plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(libs.datastore)
    implementation(projects.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}