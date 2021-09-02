package com.r3.gallery.broker.corda.client.config.mixin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.r3.payments.currency.FiatCurrency

@JsonSerialize(using = FiatCurrencySerializer::class)
abstract class FiatCurrencyMixin

internal class FiatCurrencySerializer : JsonSerializer<FiatCurrency>() {

    override fun serialize(value: FiatCurrency, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.currency.currencyCode)
    }
}