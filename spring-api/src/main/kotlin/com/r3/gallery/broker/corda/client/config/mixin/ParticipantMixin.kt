package com.r3.gallery.broker.corda.client.config.mixin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.r3.gallery.api.Participant

@JsonSerialize(using = ParticipantSerializer::class)
abstract class ParticipantMixin

internal class ParticipantSerializer : JsonSerializer<Participant>() {

    override fun serialize(value: Participant, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("displayName", value.displayName)
        gen.writeStartArray()
        for (networkId in value.networkIds) {
            gen.writeStartObject()
            gen.writeStringField("network", networkId.network)
            gen.writeStringField("x500", networkId.x500)
            gen.writeStringField("publicKey", networkId.publicKey)
            gen.writeEndObject()
        }
        gen.writeEndArray()
        gen.writeStringField("type", value.type.name)
        gen.writeEndObject()
    }
}