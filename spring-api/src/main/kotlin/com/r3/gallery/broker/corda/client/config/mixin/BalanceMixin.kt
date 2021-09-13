package com.r3.gallery.broker.corda.client.config.mixin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.r3.gallery.api.NetworkBalancesResponse.Balance

@JsonSerialize(using = BalanceSerializer::class)
abstract class BalanceMixin

internal class BalanceSerializer : JsonSerializer<Balance>() {
    override fun serialize(value: Balance, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("currencyCode", value.currencyCode)
        gen.writeNumberField("encumberedFunds", value.encumberedFunds.toDecimal())
        gen.writeNumberField("availableFunds", value.availableFunds.toDecimal())
        gen.writeEndObject()
    }
}