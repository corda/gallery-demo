package com.r3.gallery.broker.corda.client.config.mixin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import net.corda.core.contracts.UniqueIdentifier


@JsonSerialize(using = UniqueIdentifierSerializer::class)
abstract class UniqueIdentifierMixin

internal class UniqueIdentifierSerializer : JsonSerializer<UniqueIdentifier>() {

    override fun serialize(value: UniqueIdentifier, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("UUID", value.id.toString())
        gen.writeStringField("externalId", value.externalId)
        gen.writeEndObject()
    }
}