package io.github.numq.haskcore.document

internal data class DocumentException(override val message: String) : Exception(message)