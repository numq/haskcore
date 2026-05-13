plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.common.core)
    implementation(projects.service.session)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}