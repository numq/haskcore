package io.github.numq.haskcore.stack

internal enum class StackTemplate(
    val templateName: String,
    val description: String
) {
    SIMPLE(
        templateName = "simple",
        description = "Minimal project template with executable"
    ),

    SIMPLE_LIBRARY(
        templateName = "simple-library",
        description = "Minimal project template for libraries"
    ),

    HSPEC(
        templateName = "hspec",
        description = "Project with HSpec testing framework"
    ),

    SERVANT(
        templateName = "servant",
        description = "Web API project using Servant framework"
    ),

    SCOTTY(
        templateName = "scotty-hello-world",
        description = "Web application using Scotty framework"
    ),

    RIO(
        templateName = "rio",
        description = "Application using RIO monad with logging and options"
    ),

    FOUNDATION(
        templateName = "foundation",
        description = "Project with alternative Prelude (batteries included)"
    ),

    PROTOLUDE(
        templateName = "protolude",
        description = "Project using Protolude as custom Prelude"
    ),

    HAKYLL(
        templateName = "hakyll-template",
        description = "Static website compiler using Hakyll"
    ),

    HASKELETON(
        templateName = "haskeleton",
        description = "Comprehensive project skeleton for Haskell packages"
    ),

    SPOCK(
        templateName = "spock",
        description = "Lightweight web framework project"
    ),

    GTK4(
        templateName = "gkt4",
        description = "GUI application using GTK 4.0 bindings"
    ),

    TASTY(
        templateName = "tasty-discover",
        description = "Project with Tasty test framework and auto-discovery"
    ),

    QUICKCHECK(
        templateName = "quickcheck-test-framework",
        description = "Project with QuickCheck property-based testing"
    ),

    README_LHS(
        templateName = "readme-lhs",
        description = "Literate Haskell project with README"
    ),

    NEW_TEMPLATE(
        templateName = "new-template",
        description = "Default template with modern structure"
    );

    companion object {
        fun fromString(name: String): StackTemplate? {
            return entries.find {
                it.templateName.equals(name, ignoreCase = true) ||
                        it.name.equals(name, ignoreCase = true)
            }
        }

        fun availableTemplates(): List<StackTemplate> = entries.toList()

        fun default(): StackTemplate = SIMPLE
    }

    override fun toString(): String = "$templateName - $description"
}