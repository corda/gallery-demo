package com.r3.gallery.broker.corda.client.config.mixin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.r3.gallery.api.LogUpdateEntry

@JsonSerialize(using = LogUpdateEntrySerializer::class)
abstract class LogUpdateEntryMixin

internal class LogUpdateEntrySerializer : JsonSerializer<LogUpdateEntry>() {

    override fun serialize(value: LogUpdateEntry, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeString(value.associatedFlow)
        gen.writeString(value.network)
        gen.writeString(value.x500)
        gen.writeString(value.logRecordId)
        gen.writeString(value.timestamp)
        gen.writeString(value.message)
        gen.writeEndObject()
    }
}