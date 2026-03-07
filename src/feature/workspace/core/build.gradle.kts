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
    implementation(projects.service.document)
    implementation(projects.service.configuration)
    implementation(projects.service.project)
    implementation(projects.service.session)
    implementation(projects.service.text)
    implementation(projects.service.vfs)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}