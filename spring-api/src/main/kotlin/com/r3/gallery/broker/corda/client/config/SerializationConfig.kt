package com.r3.gallery.broker.corda.client.config

import com.r3.gallery.broker.corda.client.config.mixin.AmountMixin
import com.r3.gallery.broker.corda.client.config.mixin.FiatCurrencyMixin
import com.r3.gallery.broker.corda.client.config.mixin.UniqueIdentifierMixin
import com.r3.payments.currency.FiatCurrency
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import net.corda.client.jackson.JacksonSupport.createNonRpcMapper
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier


/**
 * Jackson Serialization mappings for custom Payment types. Handles encoding of JSON objects returned by
 * AgentController. To create a custom mapping, add a 'mixin' class to package config/mixin and add to mapper here.
 */
@Configuration
open class SerializationConfig {
    @Bean
    open fun mappingJackson2HttpMessageConverter(): MappingJackson2HttpMessageConverter {
        val mapper = createNonRpcMapper()
        mapper.addMixIn(UniqueIdentifier::class.java, UniqueIdentifierMixin::class.java)
        mapper.addMixIn(Amount::class.java, AmountMixin::class.java)
        mapper.addMixIn(FiatCurrency::class.java, FiatCurrencyMixin::class.java)
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = mapper
        return converter
    }
}