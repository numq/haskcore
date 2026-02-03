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

val baseModules = listOf("core")
val platformModules = listOf("application", "navigation", "ui")
val serviceModules = listOf(
    "configuration", "document", "language", "project", "runtime", "session", "toolchain", "vfs"
)
val featureModules = listOf("editor", "explorer", "output", "settings", "workspace")

baseModules.forEach { name ->
    val path = ":$name"
    include(path)
    project(path).projectDir = file("src/$name")
}

platformModules.forEach { name ->
    val path = ":platform:$name"
    include(path)
    project(path).projectDir = file("src/platform/$name")
}

serviceModules.forEach { name ->
    val path = ":service:$name"
    include(path)
    project(path).projectDir = file("src/service/$name")
}

featureModules.forEach { name ->
    val corePath = ":feature:$name:core"
    val presentationPath = ":feature:$name:presentation"
    include(corePath, presentationPath)
    project(corePath).projectDir = file("src/feature/$name/core")
    project(presentationPath).projectDir = file("src/feature/$name/presentation")
}