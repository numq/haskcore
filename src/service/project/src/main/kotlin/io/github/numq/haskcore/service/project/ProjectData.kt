package io.github.numq.haskcore.service.project

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class ProjectData(
    @ProtoNumber(1) val name: String? = null,
    @ProtoNumber(2) val openedDocumentPaths: List<String> = emptyList(),
    @ProtoNumber(3) val activeDocumentPath: String? = null
)