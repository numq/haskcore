import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinxAtomicfu)
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
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.coroutinesCore)
        implementation(libs.kotlin.test)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}