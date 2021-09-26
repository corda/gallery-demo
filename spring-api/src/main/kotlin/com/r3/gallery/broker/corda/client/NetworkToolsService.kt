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
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.hash
import net.corda.core.messaging.CordaRPCOps
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import javax.annotation.PostConstruct

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

    private lateinit var networkClients: List<ConnectionService>
    private lateinit var tokenClients: List<ConnectionService>

    // for caching fetches
    private val taskExecutor = SimpleAsyncTaskExecutor()
    private var balanceResult: CopyOnWriteArrayList<NetworkBalancesResponse>? = null
    private var logResult: CopyOnWriteArrayList<LogUpdateEntry>? = null

    @PostConstruct
    private fun postConstruct() {
        networkClients = listOf(connectionManager.auction, connectionManager.cbdc, connectionManager.gbp)
        tokenClients = listOf(connectionManager.cbdc, connectionManager.gbp)
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

    // Utility fun for aggregating results across multiple connection services
    private fun <T> List<ConnectionService>.runPerConnectionService(block: (ConnectionService) -> T): List<T> {
        return this.map(block)
    }

    /**
     * Returns participants across all networks
     *
     * Creates a list of Pairs [displayName (node identity) and NetworkId] across all networks;
     * Constructs Participants and injects grouped list
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
     * Initialisation of logService and connections on first call
     * - current implementation is all or nothing (all intended nodes must be
     * available for logService to correctly init.
     */
    fun getLogs(index: Int?): List<LogUpdateEntry> {
        logger.info("Starting log retrieval")
        var initialLatch: CountDownLatch? = null
        if (this.balanceResult == null) {
            initialLatch = CountDownLatch(1)
        }

        taskExecutor.execute {
            if (!logService.isInitialized) logService.initSubscriptions().also { logService.isInitialized = true }
            val result = logService.getProgressUpdates(index ?: 0)
            this.logResult = CopyOnWriteArrayList(result.second)
            initialLatch?.countDown()
        }

        initialLatch?.let { initialLatch.await() }
        return this.logResult!!
    }

    /**
     * Returns Balances of all parties
     */
    fun getBalance(): Map<CordaX500Name, List<CompletableFuture<NetworkBalancesResponse.Balance>>> {
        val balance  = tokenClients.runPerConnectionService {
            val network = it.associatedNetwork
            it.allConnections()!!.map { rpc ->
                val x500 = rpc.proxy.nodeInfo().legalIdentities.first().name
                val balanceFuture = runDynamicOnConnection(proxy = rpc.proxy, GetEncumberedAndAvailableBalanceFlow::class.java, currency = network.name)
                        .thenApply { currBalance -> currBalance as NetworkBalancesResponse.Balance }
                Pair(x500, balanceFuture)
            }
        }.flatten()

        return balance.groupBy { it.first }.mapValues { x500pair -> x500pair.value.map { it.second } }
    }

    /**
     * Reset or Initialize auction demo conditions
     */
    fun initializeDemo(): List<CompletableFuture<out Any>> {
        logger.info("Issuing new demo data to networks.")
        val completableFutures: MutableList<CompletableFuture<out Any>> = CopyOnWriteArrayList()
        completableFutures.addAll(clearDemo())

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

        if (!logService.isInitialized) logService.initSubscriptions()
        return completableFutures
    }

    /**
     * Consumes all relevant tokens and art to reset the auction demo state
     */
    private fun clearDemo(): List<CompletableFuture<out Any>> {
        logger.info("Clearing demo data.")
        val completableFutures: MutableList<CompletableFuture<out Any>> = CopyOnWriteArrayList()

        // destroy (off-ledger any outstanding art pieces
        connectionManager.auction.allConnections()!!.map {
            completableFutures.add(runDynamicOnConnection(it.proxy, DestroyArtwork::class.java))
        }

        // Release locks and burn tokens on GBP network
        connectionManager.gbp.allConnections()!!.map {
            completableFutures.add(runDynamicOnConnection(it.proxy, BurnTokens::class.java, "GBP"))
        }

        // Release locks and burn tokens on CBDC network
        connectionManager.cbdc.allConnections()!!.map {
            completableFutures.add(runDynamicOnConnection(it.proxy, BurnTokens::class.java, "CBDC"))
        }

        // clear logs from service
        if (logService.isInitialized) logService.clearLogs()
        return completableFutures
    }

    private inline fun <reified T: FlowLogic<Any>> runDynamicOnConnection(proxy: CordaRPCOps, clazz: Class<T>, currency: String? = null): CompletableFuture<out Any> {
        val flowHandle = if (currency != null) proxy.startFlowDynamic(clazz, currency) else proxy.startFlowDynamic(clazz)
        return flowHandle.returnValue.toCompletableFuture()
    }
}