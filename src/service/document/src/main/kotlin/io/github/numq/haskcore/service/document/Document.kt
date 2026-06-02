package io.github.numq.haskcore.service.document

data class Document(
    val metadata: Metadata,
    val content: String,
    val isModified: Boolean,
)