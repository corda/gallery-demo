package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.art.service.NodeClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller with aggregate access across  API layers
 * for x-network queries and generic operations.
 */
@CrossOrigin
@RestController
@RequestMapping("/network")
class NetworkToolsController(
    artNetworkGalleryClient: ArtNetworkGalleryClient,
    artNetworkBidderClient: ArtNetworkBidderClient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NetworkToolsController::class.java)
        const val TIMEOUT = NodeClient.TIMEOUT
    }

    private var networkClients: MutableList<NodeClient> = mutableListOf(
        artNetworkGalleryClient as NodeClient,
        artNetworkBidderClient as NodeClient
    )

    /**
     * Converts String network list to ENUM representation
     */
    private fun networksToEnum(networks: List<String>) : List<CordaRPCNetwork> =
        networks.map {
            when (it.toLowerCase()) {
                "auction" -> CordaRPCNetwork.AUCTION
                "gbp" -> CordaRPCNetwork.GBP
                "cbdc" -> CordaRPCNetwork.CBDC
                else -> throw IllegalArgumentException("bad networks queryParam")
            }
        }

    /**
     * TODO: add jackson model for NodeInfo rather than string
     */
    @GetMapping("/nodes")
    suspend fun nodes(
        @RequestParam("networks", required = false) networks: List<String>?
    ): ResponseEntity<String> {
        val nodes = networkClients.flatMap {
            it.getNodes(networks?.let { networksToEnum(networks) }, dev = true)
        }
        return ResponseEntity.status(HttpStatus.OK).body(nodes.toString())
    }
}