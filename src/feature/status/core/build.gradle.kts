plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(projects.core)
    implementation(projects.service.project)
    implementation(projects.service.toolchain)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}