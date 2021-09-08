package com.r3.gallery.broker.corda.client

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.broker.services.LogRetrievalIdx
import com.r3.gallery.broker.services.LogService
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.node.NodeInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

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

        initializeLogService()
    }

    /**
     * Setup the logging service for the associated connection services
     */
    private fun initializeLogService() {
        val proxiesAndNetwork = networkClients.runPerConnectionService {
            val network = it.associatedNetwork
            it.sessions.values.map { rpc ->
                Pair(rpc.proxy, network!!)
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
     * Test endpoint to return NodeInfo of connections
     */
    fun nodes(networks: List<String>?) : List<NodeInfo> {
        return networkClients.runPerConnectionService {
            it.getNodes(networks?.let { networksToEnum(networks) }, dev = true)
        }.flatten()
    }

    /**
     * Log returns progressUpdates for Node Level state-machine updates
     */
    fun getLogs(): List<LogUpdateEntry> {
        val result = logService.getProgressUpdates(logIdx)
        logIdx = result.first // set indexing for next fetch
        return result.second
    }
}