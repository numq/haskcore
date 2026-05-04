plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(projects.common.core)
    implementation(projects.api.project)
    implementation(projects.api.toolchain)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}