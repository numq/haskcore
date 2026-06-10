package io.github.numq.haskcore.feature.editor.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class EditorData(
    @ProtoNumber(1) val position: EditorPositionData = EditorPositionData(),
)