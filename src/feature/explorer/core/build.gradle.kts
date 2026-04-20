plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(libs.datastore)
    implementation(projects.common.core)
    implementation(projects.api.document)
    implementation(projects.api.project)
    implementation(projects.api.vfs)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}