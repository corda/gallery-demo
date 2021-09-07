package com.r3.gallery.broker

import com.r3.gallery.broker.corda.rpc.config.RpcProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(RpcProperties::class)
class GalleryBrokerApplication

fun main(args: Array<String>) {
	runApplication<GalleryBrokerApplication>(*args)
}
