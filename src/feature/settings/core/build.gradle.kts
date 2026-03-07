plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(projects.core)
    implementation(projects.service.configuration)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}