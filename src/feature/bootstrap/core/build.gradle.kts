plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.koin.core)
    implementation(projects.core)
    implementation(projects.service.session)
    implementation(projects.service.text)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}