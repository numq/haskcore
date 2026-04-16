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

val rootModules = listOf("api", "common", "feature", "platform")

val apiModules = listOf(
    "clipboard",
    "configuration",
    "document",
    "journal",
    "keymap",
    "logger",
    "lsp",
    "project",
    "runtime",
    "session",
    "syntax",
    "text",
    "toolchain",
    "vfs"
)

val commonModules = listOf("core", "presentation")

val featureModules = listOf(
    "bootstrap",
    "editor",
    "execution",
    "explorer",
    "log",
    "navigation",
    "output",
    "settings",
    "shelf",
    "status",
    "welcome",
    "workspace"
)

rootModules.forEach { module ->
    val path = ":$module"
    include(path)
    project(path).projectDir = file("src/$module")
}

apiModules.forEach { moduleName ->
    val path = ":api:$moduleName"
    include(path)
    project(path).projectDir = file("src/api/$moduleName")
}

commonModules.forEach { moduleName ->
    val path = ":common:$moduleName"
    include(path)
    project(path).projectDir = file("src/common/$moduleName")
}

featureModules.forEach { moduleName ->
    val featureParent = ":feature:$moduleName"
    include(featureParent)
    project(featureParent).projectDir = file("src/feature/$moduleName")

    val core = "$featureParent:core"
    val pres = "$featureParent:presentation"
    include(core, pres)
    project(core).projectDir = file("src/feature/$moduleName/core")
    project(pres).projectDir = file("src/feature/$moduleName/presentation")
}