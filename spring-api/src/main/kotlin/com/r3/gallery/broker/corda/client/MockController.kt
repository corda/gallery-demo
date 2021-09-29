package com.r3.gallery.broker.corda.client

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.GBP
import com.r3.gallery.api.*
import com.r3.gallery.api.AvailableArtwork.BidRecord
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import net.corda.core.contracts.Amount
import net.corda.core.crypto.secureRandomBytes
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * MOCK controller for Gallery parties on Auction Network
 *
 * Use this class to test in deployment proposed changes or static return values against a mock controller.
 * Setting a flag in the application.properties file will deactivate all other controllers and process requests here.
 *
 * See application.properties, mock.controller.enabled property for setting this controller to active.
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "true")
@RequestMapping("/")
class MockController {
    companion object {
        private val logger = LoggerFactory.getLogger(MockController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }
}