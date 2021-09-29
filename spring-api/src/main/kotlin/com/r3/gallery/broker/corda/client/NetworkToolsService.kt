package com.r3.gallery.broker.corda.client

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.api.NetworkBalancesResponse
import com.r3.gallery.api.Participant
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.broker.services.LogService
import com.r3.gallery.workflows.artwork.DestroyArtwork
import com.r3.gallery.workflows.token.BurnTokens
import com.r3.gallery.workflows.webapp.GetEncumberedAndAvailableBalanceFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.hash
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.annotation.PostConstruct

/**
 * A class which processes multi-network utility functions and services [NetworkToolsController]
 */
@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "false")
@Service
class NetworkToolsService(
    @Autowired private val connectionManager: ConnectionManager,
    @Autowired private val artNetworkGalleryClient: ArtNetworkGalleryClient,
    @Autowired private val tokenNetworkBuyerClient: TokenNetworkBuyerClient,
    @Autowired private val logService: LogService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NetworkToolsService::class.java)
        const val ALICE = "O=Alice, L=London, C=GB"
        const val BOB = "O=Bob, L=San Francisco, C=US"
        const val CHARLIE = "O=Charlie, L=Mumbai, C=IN"
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    private val balanceCache: MutableMap<CordaX500Name, List<CompletableFuture<NetworkBalancesResponse.Balance>>> = ConcurrentHashMap()

    private lateinit var networkClients: List<ConnectionService>
    private lateinit var tokenClients: List<ConnectionService>

    @PostConstruct
    private fun postConstruct() {
        networkClients = listOf(connectionManager.auction, connectionManager.cbdc, connectionManager.gbp)
        tokenClients = listOf(connectionManager.cbdc, connectionManager.gbp)
    }

    /**
     * Converts a String network list to ENUM representation
     *
     * @param networks to convert
     * @return [List][CordaRPCNetwork] generated from param.
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

    /** Utility fun for aggregating results across multiple connection services */
    private fun <T> List<ConnectionService>.runPerConnectionService(block: (ConnectionService) -> T): List<T> {
        return this.map(block)
    }

    /**
     * Returns participants across all networks
     *
     * Creates a list of Pairs [displayName (node identity) and NetworkId] across all networks;
     * Constructs Participants and injects grouped list
     *
     * @param networks Optional list of network identifiers to filter the results on.
     * @return [List][Participant]
     */
    fun participants(networks: List<String>?) : List<Participant> {
        logger.info("Attempting to fetch participants from all networks: ${CordaRPCNetwork.values()}")
        val allNetworkIds = networkClients.runPerConnectionService {
            val currentNetwork = it.associatedNetwork.name
            logger.info("Polling participants from $currentNetwork")
            it.getNodes(networks?.let { networksToEnum(networks) }, dev = true)
                .map { nodeInfo ->
                    val x500 = nodeInfo.legalIdentitiesAndCerts.first().name
                    val pubicKey = nodeInfo.legalIdentitiesAndCerts.first().owningKey.hash.toHexString()
                    Pair(x500, Participant.NetworkId(currentNetwork, pubicKey))
                }

        }.flatten()
        return allNetworkIds.groupBy { it.first }
            .entries.map {
                val x500 = it.key
                val displayName = it.key.organisation
                // TODO: remove hardcode of 'type'
                Participant(
                    displayName = displayName,
                    x500 = x500.toString(),
                    it.value.map { list -> list.second },
                    if (displayName.contains("alice", true))
                        Participant.AuctionRole.GALLERY else Participant.AuctionRole.BIDDER
                )
            }
    }

    /**
     * Log returns progressUpdates for Node Level state-machine updates
     *
     * @param index Optional position to start returned logs from.
     * @return [List][LogUpdateEntry]
     */
    fun getLogs(index: Int?): List<LogUpdateEntry> {
        logger.info("Starting log retrieval")
        return logService.getProgressUpdates(index ?: 0)
    }

    /**
     * Returns Balances of all parties on each network they belong, with categories for encumbered and available tokens.
     *
     * @return [Map] k - CordaX500Name, v - list of futures which will resolve to Balances held, for each network identity of key.
     */
    fun getBalance(): Map<CordaX500Name, List<CompletableFuture<NetworkBalancesResponse.Balance>>> {
        return if (balanceCache.isNotEmpty()) balanceCache.also {
            updateBalanceCache()
        } else updateBalanceCache().let { balanceCache }
    }

    /** Helper function to update n-1 result cache for faster polling response */
    private fun updateBalanceCache() {
        val balance  = tokenClients.runPerConnectionService {
            it.allProxies()!!.map { connection ->
                val x500 = connection.key
                val (network, proxy) = connection.value
                val balanceFuture = proxy.startFlowDynamic(GetEncumberedAndAvailableBalanceFlow::class.java, network.name).returnValue.toCompletableFuture()
                        .thenApply { currBalance -> currBalance as NetworkBalancesResponse.Balance }
                Pair(x500, balanceFuture)
            }
        }.flatten()

        balanceCache.clear()
        balanceCache.putAll(balance.groupBy { it.first }.mapValues { x500pair -> x500pair.value.map { it.second } })
    }

    /**
     * Reset or Initialize auction demo conditions. Will clear existing art and currency states including any
     * encumbered tokens, and then re-issue to default amounts of (8000 GBP, 5000 CBDC)
     *
     * @return [List][CompletableFuture] to return issuance results on completion.
     */
    fun initializeDemo(): List<CompletableFuture<out Any>> {
        logger.info("Issuing new demo data to networks.")
        val completableFutures: MutableList<CompletableFuture<out Any>> = CopyOnWriteArrayList()

        // clear existing data
        clearDemo()

        // artworks
        val urlPrefix = "/assets/artwork/"
        listOf(
            Pair("A Thousand Plateaus", "A_Thousand_Plateaus.png"),
            Pair("Cities of the Red Night", "Cities_of_the_Red_Night.png"),
            Pair("All Watched Over By Machines", "All_Watched_Over_By_Machines_Of_Loving_Grace.png"),
            Pair("The Eerie Bliss", "The_Eerie_Bliss_and_Torture_of_Solitude.png"),
            Pair("The Masque of the Red Death", "The_Masque_of_the_Red_Death.png")
        ).forEach { // issue with default expiry of 3 days.
            completableFutures.add(artNetworkGalleryClient.issueArtwork(
                galleryParty = ALICE,
                artworkId = UUID.randomUUID(),
                description = it.first,
                url = urlPrefix+it.second
            ).toCompletableFuture())
        }

        // GBP issued to Bob
        completableFutures.add(tokenNetworkBuyerClient.issueTokens(BOB, 500000, "GBP").toCompletableFuture())
        // CBDC issued to Charlie
        completableFutures.add(tokenNetworkBuyerClient.issueTokens(CHARLIE, 8000, "CBDC").toCompletableFuture())

        logService.clearLogs()
        logService.subscribeToRpcConnectionStateMachines()
        return completableFutures
    }

    /**
     * Consumes all relevant tokens and art to reset the auction demo state
     */
    private fun clearDemo() {
        logger.info("Clearing demo data.")
        // destroy (off-ledger any outstanding art pieces
        connectionManager.auction.allProxies()!!.map {
            it.value.second.startFlowDynamic(DestroyArtwork::class.java).returnValue.getOrThrow()
        }

        // Release locks and burn tokens on GBP network
        connectionManager.gbp.allProxies()!!.map {
            it.value.second.startFlowDynamic(BurnTokens::class.java, "GBP").returnValue.getOrThrow()
        }

        // Release locks and burn tokens on CBDC network
        connectionManager.cbdc.allProxies()!!.map {
            it.value.second.startFlowDynamic(BurnTokens::class.java, "CBDC").returnValue.getOrThrow()
        }
    }
}