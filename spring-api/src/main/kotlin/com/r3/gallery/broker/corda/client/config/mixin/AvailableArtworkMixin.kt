package com.r3.gallery.broker.corda.client.config.mixin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.r3.gallery.api.AvailableArtwork

@JsonSerialize(using = AvailableArtworkSerializer::class)
abstract class AvailableArtworkMixin

internal class AvailableArtworkSerializer : JsonSerializer<AvailableArtwork>() {
    override fun serialize(value: AvailableArtwork, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("artworkId", value.artworkId.toString())
        gen.writeStringField("description", value.description)
        gen.writeStringField("url", value.url)
        gen.writeBooleanField("listed", value.listed)
        gen.writeArrayFieldStart("bids")
        for (bid in value.bids) {
            gen.writeStartObject()
            gen.writeStringField("cordaReference", bid.cordaReference.toString())
            gen.writeStringField("bidderPublicKey", bid.bidderPublicKey)
            gen.writeStringField("bidderDisplayName", bid.bidderDisplayName)
            gen.writeStringField("amount", bid.amountAndCurrency.toDecimal().toString())
            gen.writeStringField("currencyCode", bid.amountAndCurrency.token.tokenIdentifier)
            gen.writeStringField("notary", bid.notary)
            gen.writeStringField("expiryDate", bid.expiryDate.toString())
            gen.writeBooleanField("accepted", bid.accepted)
            gen.writeEndObject()
        }
        gen.writeEndArray()
        gen.writeEndObject()
    }
}