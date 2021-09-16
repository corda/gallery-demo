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
        gen.writeStringField("associatedFlow", value.associatedFlow)
        gen.writeStringField("network", value.network)
        gen.writeStringField("x500", value.x500)
        gen.writeStringField("logRecordId", value.logRecordId)
        gen.writeStringField("timestamp", value.timestamp)
        gen.writeStringField("message", value.message)
        if (value.completed != null) { // FlowCompletionLog
            gen.writeObjectFieldStart("completed")
            gen.writeStringField("associatedStage", value.completed!!.associatedStage)
            gen.writeStringField("logRecordId", value.completed!!.logRecordId)
            gen.writeObjectField("states", value.completed!!.states)
            gen.writeObjectField("signers", value.completed!!.signers)
            gen.writeEndObject()
        }
        gen.writeEndObject()
    }
}