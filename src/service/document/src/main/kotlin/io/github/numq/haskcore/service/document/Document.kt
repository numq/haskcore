package io.github.numq.haskcore.service.document

data class Document(val path: String, val name: String, val content: String, val isModified: Boolean)