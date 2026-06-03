plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.common.core)
    implementation(projects.service.lsp)
    implementation(projects.service.session)
    implementation(projects.service.syntax)
    implementation(projects.service.toolchain)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}