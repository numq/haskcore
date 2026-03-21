package io.github.numq.haskcore.service.document

data class Document(
    val path: String, val name: String, val extension: String, val content: String, val isModified: Boolean
) {
    private companion object {
        val HASKELL_EXTENSIONS = setOf("hs", "lhs")
    }

    val isHaskell get() = extension.lowercase() in HASKELL_EXTENSIONS
}