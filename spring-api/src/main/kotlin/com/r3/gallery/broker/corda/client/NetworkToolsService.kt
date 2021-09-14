package com.r3.gallery.broker.corda.client

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.api.Participant
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.broker.services.LogRetrievalIdx
import com.r3.gallery.broker.services.LogService
import com.r3.gallery.broker.services.exceptions.LogInitializationError
import net.corda.client.rpc.CordaRPCConnection
import net.corda.client.rpc.RPCException
import net.corda.core.internal.hash
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "false")
@Component
class NetworkToolsService {
    companion object {
        private val logger = LoggerFactory.getLogger(NetworkToolsService::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    private var networkClients: MutableList<ConnectionService> = ArrayList()
    private lateinit var logService: LogService
    private var logIdx: LogRetrievalIdx = 0

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
     * Setup the logging service for the associated connection services
     */
    private fun initializeLogService() {
        val proxiesAndNetwork = networkClients.runPerConnectionService connect@{
            val network = it.associatedNetwork

            return@connect try {
                it.allConnections()?.map { rpc ->
                    Pair(rpc.proxy, network!!)
                }!!
            } catch (rpcE: RPCException) {
                val issue = "Unable to connect to a target node: $rpcE"
                logger.error(issue)
                throw LogInitializationError(issue)
            }

        }.flatten()

        logService = LogService(proxiesAndNetwork)
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

    // Utility fun for executing block across all connections contained in the calling list of services
    private fun <T> List<ConnectionService>.runPerRPCConnection(block: (CordaRPCConnection) -> T): List<T> {
        return this.flatMap {
            it.sessions.values.map(block)
        }
    }

    /**
     * Returns participants across all networks
     *
     * Creates a list of Pairs [displayName (node identity) and NetworkId] across all networks;
     * Constructs Participants and injects grouped list
     */
    fun participants(networks: List<String>?) : List<Participant> {
        val allNetworkIds = networkClients.runPerConnectionService {
            val currentNetwork = it.associatedNetwork!!.netName
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
    fun getLogs(): List<LogUpdateEntry> {
        if (!this::logService.isInitialized) {
            initializeLogService()
        }
        val result = logService.getProgressUpdates(logIdx)
        logIdx = result.first // set indexing for next fetch
        return result.second
    }
}