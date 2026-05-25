plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.common.core)
    implementation(projects.service.session)
    implementation(projects.service.syntax)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}