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

/**
 * Pure Kotlin
 */
include(":compiler")
include(":core")
include(":filesystem")
include(":keymap")
include(":lsp")
include(":project")
include(":run")
include(":settings")
include(":workspace")

/**
 * Compose
 */
include(":application")
include(":editor")
include(":explorer")
include(":ui")
include(":window")