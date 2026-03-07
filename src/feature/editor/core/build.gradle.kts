plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.atomicfu)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(libs.java.diff.utils)
    implementation(projects.core)
    implementation(projects.service.document)
    implementation(projects.service.journal)
    implementation(projects.service.keymap)
    implementation(projects.service.text)
    implementation(projects.service.vfs)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}