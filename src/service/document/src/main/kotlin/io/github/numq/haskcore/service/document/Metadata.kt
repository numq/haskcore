package io.github.numq.haskcore.service.document

import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.core.text.TextEncoding

data class Metadata(
    val path: String,
    val name: String,
    val extension: String,
    val encoding: TextEncoding,
) {
    val language: Language by lazy {
        Language.fromExtension(path = path)
    }
}