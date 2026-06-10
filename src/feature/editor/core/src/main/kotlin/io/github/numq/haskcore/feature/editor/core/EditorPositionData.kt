package io.github.numq.haskcore.feature.editor.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class EditorPositionData(
    @ProtoNumber(1) val horizontalOffset: Float = 0f,
    @ProtoNumber(2) val verticalOffset: Float = 0f,
)