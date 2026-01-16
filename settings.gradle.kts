rootProject.name = "haskcore"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val platform = setOf("application", "core", "navigation", "ui")

val services = setOf("language", "runtime", "toolchain", "vfs")

val modules = platform + services

modules.forEach { module ->
    include(":$module")

    project(":$module").projectDir = when (module) {
        in platform -> file("src/platform/$module")

        else -> file("src/service/$module")
    }
}

val features = setOf("document", "explorer", "output", "settings", "window", "workspace")

features.filterNot(modules::contains).forEach { feature ->
    val core = ":$feature:core"
    include(core)
    project(core).projectDir = file("src/feature/$feature/core")

    val presentation = ":$feature:presentation"
    include(presentation)
    project(presentation).projectDir = file("src/feature/$feature/presentation")
}