package com.r3.gallery.broker.corda.client.config.mixin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@JsonSerialize(using = ContractStateSerializer::class)
abstract class ContractStateMixin

internal class ContractStateSerializer : JsonSerializer<ContractState>() {

    @Suppress("UNCHECKED_CAST")
    override fun serialize(value: ContractState, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectFieldStart("properties")
        (value::class as KClass<Any>).memberProperties.forEach { member ->
            member.isAccessible = true
            gen.writeStringField(member.name, member.get(value).toString())
        }
        gen.writeEndObject()
        gen.writeArrayFieldStart("participants")
        for (participant in value.participants) {
            val party = if (participant is AnonymousParty) participant else (participant as Party)
            gen.writeString(party.toString())
        }
        gen.writeEndArray()
        gen.writeEndObject()
    }
}