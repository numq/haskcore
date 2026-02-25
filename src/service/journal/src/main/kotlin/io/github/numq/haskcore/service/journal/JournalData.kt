package io.github.numq.haskcore.service.journal

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class JournalData(
    @ProtoNumber(1) val records: List<JournalRecordData> = emptyList(), @ProtoNumber(2) val currentIndex: Int = -1
)