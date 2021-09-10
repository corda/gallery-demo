package com.r3.gallery.broker.corda.client

import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.api.Participant
import com.r3.gallery.broker.corda.client.art.controllers.asResponse
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller with aggregate access across  API layers
 * for x-network queries and generic operations.
 */
@CrossOrigin
@RestController
@RequestMapping("/network")
class NetworkToolsController(@Autowired private val networkToolsService: NetworkToolsService) {

    companion object {
        private val logger = LoggerFactory.getLogger(NetworkToolsController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    /**
     * Test endpoint to return NodeInfo of connections
     * TODO: add jackson model for NodeInfo rather than string
     */
    @GetMapping("/participants")
    fun participants(
        @RequestParam("networks", required = false) networks: List<String>?
    ): ResponseEntity<List<Participant>> {
        return asResponse(networkToolsService.participants(networks))
    }

    /**
     * Log returns progressUpdates for Node Level state-machine updates
     */
    @GetMapping("/log")
    fun log(): ResponseEntity<List<LogUpdateEntry>> {
        return asResponse(networkToolsService.getLogs())
    }
}