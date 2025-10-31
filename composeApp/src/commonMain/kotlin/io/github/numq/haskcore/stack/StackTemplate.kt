package io.github.numq.haskcore.stack

internal enum class StackTemplate(val template: String, val description: String) {
    SIMPLE("simple", "Minimal project template with executable"), SIMPLE_LIBRARY(
        "simple-library",
        "Minimal project template for libraries"
    ),
    HSPEC("hspec", "Project with HSpec testing framework"), SERVANT(
        "servant",
        "Web API project using Servant framework"
    ),
    SCOTTY("scotty-hello-world", "Web application using Scotty framework"), RIO(
        "rio",
        "Application using RIO monad with logging and options"
    ),
    FOUNDATION("foundation", "Project with alternative Prelude"), PROTOLUDE(
        "protolude",
        "Project using Protolude as custom Prelude"
    ),
    HAKYLL("hakyll-template", "Static website compiler using Hakyll"), HASKELETON(
        "haskeleton",
        "Comprehensive project skeleton"
    ),
    SPOCK("spock", "Lightweight web framework project"), GTK4(
        "gtk4",
        "GUI application using GTK 4.0 bindings"
    ),
    TASTY("tasty-discover", "Project with Tasty test framework"), QUICKCHECK(
        "quickcheck-test-framework",
        "Project with QuickCheck testing"
    ),
    README_LHS("readme-lhs", "Literate Haskell project with README"), NEW_TEMPLATE(
        "new-template",
        "Default template with modern structure"
    )
}