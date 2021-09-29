package com.r3.gallery.broker.corda.client.config

import com.r3.gallery.api.AvailableArtwork
import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.api.NetworkBalancesResponse
import com.r3.gallery.api.Participant
import com.r3.gallery.broker.corda.client.config.mixin.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import net.corda.client.jackson.JacksonSupport.createNonRpcMapper
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier


/**
 * Jackson Serialization mappings for custom types. Handles encoding of JSON objects returned by
 * Gallery Cordapp Controllers. To create a custom mapping, add a 'mixin' class to package config/mixin and add to mapper here.
 */
@Configuration
class SerializationConfig {
    @Bean
    fun mappingJackson2HttpMessageConverter(): MappingJackson2HttpMessageConverter {
        val mapper = createNonRpcMapper()
        mapper.addMixIn(UniqueIdentifier::class.java, UniqueIdentifierMixin::class.java)
        mapper.addMixIn(Amount::class.java, AmountMixin::class.java)
        mapper.addMixIn(LogUpdateEntry::class.java, LogUpdateEntryMixin::class.java)
        mapper.addMixIn(Participant::class.java, ParticipantMixin::class.java)
        mapper.addMixIn(NetworkBalancesResponse.Balance::class.java, BalanceMixin::class.java)
        mapper.addMixIn(AvailableArtwork::class.java, AvailableArtworkMixin::class.java)
        mapper.addMixIn(ContractState::class.java, ContractStateMixin::class.java)
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = mapper
        return converter
    }
}