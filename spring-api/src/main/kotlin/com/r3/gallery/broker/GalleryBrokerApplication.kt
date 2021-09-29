package com.r3.gallery.broker

import com.r3.gallery.broker.corda.rpc.config.RpcProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@SpringBootApplication
@EnableConfigurationProperties(RpcProperties::class)
class GalleryBrokerApplication

/**
 * Spring controller entry point - run to bring up the swaps orchestration service.
 */
fun main(args: Array<String>) {
	runApplication<GalleryBrokerApplication>(*args)
}
