package io.github.numq.haskcore.service.journal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal sealed interface JournalRecordData {
    val revision: Long

    val startByte: Int

    val oldEndByte: Int

    val newEndByte: Int

    val startLine: Int

    val startColumn: Int

    val oldEndLine: Int

    val oldEndColumn: Int

    val newEndLine: Int

    val newEndColumn: Int

    val timestampNanos: Long

    @Serializable
    @SerialName("insert")
    data class Insert(
        @ProtoNumber(1) override val revision: Long,
        @ProtoNumber(2) override val startByte: Int,
        @ProtoNumber(3) override val newEndByte: Int,
        @ProtoNumber(4) override val startLine: Int,
        @ProtoNumber(5) override val startColumn: Int,
        @ProtoNumber(6) override val newEndLine: Int,
        @ProtoNumber(7) override val newEndColumn: Int,
        @ProtoNumber(8) override val timestampNanos: Long,
        @ProtoNumber(9) val text: String
    ) : JournalRecordData {
        override val oldEndByte get() = startByte

        override val oldEndLine get() = startLine

        override val oldEndColumn get() = startColumn
    }

    @Serializable
    @SerialName("replace")
    data class Replace(
        @ProtoNumber(1) override val revision: Long,
        @ProtoNumber(2) override val startByte: Int,
        @ProtoNumber(3) override val oldEndByte: Int,
        @ProtoNumber(4) override val newEndByte: Int,
        @ProtoNumber(5) override val startLine: Int,
        @ProtoNumber(6) override val startColumn: Int,
        @ProtoNumber(7) override val oldEndLine: Int,
        @ProtoNumber(8) override val oldEndColumn: Int,
        @ProtoNumber(9) override val newEndLine: Int,
        @ProtoNumber(10) override val newEndColumn: Int,
        @ProtoNumber(11) override val timestampNanos: Long,
        @ProtoNumber(12) val oldText: String,
        @ProtoNumber(13) val newText: String
    ) : JournalRecordData

    @Serializable
    @SerialName("delete")
    data class Delete(
        @ProtoNumber(1) override val revision: Long,
        @ProtoNumber(2) override val startByte: Int,
        @ProtoNumber(3) override val oldEndByte: Int,
        @ProtoNumber(4) override val startLine: Int,
        @ProtoNumber(5) override val startColumn: Int,
        @ProtoNumber(6) override val oldEndLine: Int,
        @ProtoNumber(7) override val oldEndColumn: Int,
        @ProtoNumber(8) override val timestampNanos: Long,
        @ProtoNumber(9) val text: String
    ) : JournalRecordData {
        override val newEndByte get() = startByte

        override val newEndLine get() = startLine

        override val newEndColumn get() = startColumn
    }

    @Serializable
    @SerialName("batch")
    data class Batch(
        @ProtoNumber(1) override val revision: Long,
        @ProtoNumber(2) override val timestampNanos: Long,
        @ProtoNumber(3) val records: List<JournalRecordData>
    ) : JournalRecordData {
        override val startByte get() = records.first().startByte

        override val oldEndByte get() = records.last().oldEndByte

        override val newEndByte get() = records.last().newEndByte

        override val startLine get() = records.first().startLine

        override val startColumn get() = records.first().startColumn

        override val oldEndLine get() = records.last().oldEndLine

        override val oldEndColumn get() = records.last().oldEndColumn

        override val newEndLine get() = records.last().newEndLine

        override val newEndColumn get() = records.last().newEndColumn
    }
}