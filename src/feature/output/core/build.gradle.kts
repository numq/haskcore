plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.datastore)
    implementation(projects.common.core)
    implementation(projects.service.clipboard)
    implementation(projects.service.runtime)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}