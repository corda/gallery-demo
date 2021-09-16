package com.r3.gallery.broker.corda.client

import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.api.NetworkBalancesResponse
import com.r3.gallery.api.Participant
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.art.controllers.asResponse
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Controller with aggregate access across  API layers
 * for x-network queries and generic operations.
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "false")
@RequestMapping("/network")
class NetworkToolsController(
    @Autowired private val networkToolsService: NetworkToolsService,
    @Autowired private val galleryClient: ArtNetworkGalleryClient,
    @Autowired private val tokenClient: TokenNetworkBuyerClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(NetworkToolsController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
        const val ALICE = "O=Alice,L=London,C=GB"
        const val BOB = "O=Bob,L=San Francisco,C=US"
        const val CHARLIE = "O=Charlie,L=Mumbai,C=IN"
    }

    /**
     * Test endpoint to return NodeInfo of connections
     * TODO: add jackson model for NodeInfo rather than string
     */
    @GetMapping("/participants")
    fun participants(
        @RequestParam("networks", required = false) networks: List<String>?
    ): ResponseEntity<List<Participant>> {
        logger.info("Request for all participants")
        return asResponse(networkToolsService.participants(networks))
    }

    /**
     * Log returns progressUpdates for Node Level state-machine updates
     */
    @GetMapping("/log")
    fun log(): ResponseEntity<List<LogUpdateEntry>> {
        logger.info("Request for logs")
        return asResponse(networkToolsService.getLogs())
    }

    /**
     * Get balances across all parties and networks
     */
    @GetMapping("/balance")
    fun balance(): ResponseEntity<List<NetworkBalancesResponse>> {
        logger.info("Request for balance of parties across network")
        return asResponse(networkToolsService.getBalance())
    }

    /**
     * Initialise the initial artworks and correct amount of funds to the demo parties
     */
    @GetMapping("/init")
    fun initIssuance(): ResponseEntity<Unit> {
        logger.info("Request for initial issuance to networks.")

        // artworks
        val urlPrefix = "/assets/artwork"
        listOf(
            Pair("A Thousand Plateaus", "A_Thousand_Plateaus.png"),
            Pair("Cities of the Red Night", "Cities_of_the_Red_Night.png"),
            Pair("The Funeral of Being", "The_Funeral_of_Being.png"),
            Pair("All Watched Over By Machines", "All_Watched_Over_By_Machines_Of_Loving_Grace.png"),
            Pair("The Eerie Bliss", "The_Eerie_Bliss_and_Torture_of_Solitude.png"),
            Pair("The Masque of the Red Death", "The_Masque_of_the_Red_Death.png")
        ).forEach {
            galleryClient.issueArtwork(ALICE, UUID.randomUUID(), it.first, urlPrefix+it.second)
        }

        // GBP to BOB
        tokenClient.issueTokens(BOB, 5000, "GBP")

        // CBDC to CHARLIE

        return asResponse(Unit)
    }
}