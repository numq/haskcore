rootProject.name = "haskcore"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val rootModules = listOf("common", "entrypoint", "feature", "service", "platform")

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
    "status",
    "welcome",
    "workspace"
)

val serviceModules = listOf(
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

rootModules.forEach { module ->
    val path = ":$module"
    include(path)
    project(path).projectDir = file("src/$module")
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

serviceModules.forEach { moduleName ->
    val path = ":service:$moduleName"
    include(path)
    project(path).projectDir = file("src/service/$moduleName")
}