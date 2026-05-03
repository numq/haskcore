plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(projects.common.core)
    implementation(projects.api.configuration)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}