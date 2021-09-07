package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct

/**
 * Controller with aggregate access across  API layers
 * for x-network queries and generic operations.
 */
@CrossOrigin
@RestController
@RequestMapping("/network")
class NetworkToolsController {

    companion object {
        private val logger = LoggerFactory.getLogger(NetworkToolsController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    private var networkClients: MutableList<ConnectionServiceImpl> = ArrayList()

    @Autowired
    @Qualifier("ArtNetworkGalleryProperties")
    private lateinit var artNetworkGalleryProperties: ClientProperties

    @Autowired
    @Qualifier("ArtNetworkBidderProperties")
    private lateinit var artNetworkBidderProperties: ClientProperties

    // init client and set associated network
    @PostConstruct
    private fun postConstruct() {
        val artNetworkGalleryCS = ConnectionServiceImpl(artNetworkGalleryProperties)
        artNetworkGalleryCS.associatedNetwork = CordaRPCNetwork.AUCTION

        val artNetworkBidderCS = ConnectionServiceImpl(artNetworkBidderProperties)
        artNetworkBidderCS.associatedNetwork = CordaRPCNetwork.AUCTION

        networkClients.add(artNetworkBidderCS)
        networkClients.add(artNetworkGalleryCS)
    }

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
    fun nodes(
        @RequestParam("networks", required = false) networks: List<String>?
    ): ResponseEntity<String> {
        val nodes = networkClients.flatMap {
            it.getNodes(networks?.let { networksToEnum(networks) }, dev = true)
        }
        return ResponseEntity.status(HttpStatus.OK).body(nodes.toString())
    }
}