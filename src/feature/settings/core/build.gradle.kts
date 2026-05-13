plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.common.core)
    implementation(projects.service.configuration)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}