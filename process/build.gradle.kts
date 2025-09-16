import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
    }

    dependencies {
        implementation(project(":core"))
        implementation(libs.kotlinx.coroutinesCore)
        implementation(libs.koin.core)
        implementation(libs.kotlin.test)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}