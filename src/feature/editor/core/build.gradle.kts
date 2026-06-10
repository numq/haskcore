plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.atomicfu)
}

dependencies {
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.datastore)
    implementation(projects.common.core)
    implementation(projects.service.clipboard)
    implementation(projects.service.document)
    implementation(projects.service.journal)
    implementation(projects.service.keymap)
    implementation(projects.service.logger)
    implementation(projects.service.lsp)
    implementation(projects.service.syntax)
    implementation(projects.service.text)
    implementation(projects.service.toolchain)
    implementation(projects.service.vfs)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}