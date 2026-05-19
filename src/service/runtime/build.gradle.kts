plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.common.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}