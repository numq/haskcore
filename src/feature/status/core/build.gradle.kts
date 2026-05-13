plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.common.core)
    implementation(projects.service.project)
    implementation(projects.service.toolchain)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}