package com.r3.gallery.broker.corda.client.config.mixin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount

@JsonSerialize(using = AmountSerializer::class)
abstract class AmountMixin

internal class AmountSerializer : JsonSerializer<Amount<TokenType>>() {
    override fun serialize(value: Amount<TokenType>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("currencyCode", value.token.tokenIdentifier)
        gen.writeNumberField("amount", value.toDecimal())
        gen.writeEndObject()
    }
}