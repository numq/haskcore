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

val featureSubmodules = listOf("core", "presentation")

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

    featureSubmodules.forEach { submoduleName ->
        val submodule = "$featureParent:$submoduleName"
        include(submodule)
        project(submodule).projectDir = file("src/feature/$moduleName/$submoduleName")
    }
}

serviceModules.forEach { moduleName ->
    val path = ":service:$moduleName"
    include(path)
    project(path).projectDir = file("src/service/$moduleName")
}