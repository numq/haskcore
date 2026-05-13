plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.datastore)
    implementation(projects.common.core)
    implementation(projects.service.configuration)
    implementation(projects.service.document)
    implementation(projects.service.runtime)
    implementation(projects.service.toolchain)
    implementation(projects.service.vfs)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}