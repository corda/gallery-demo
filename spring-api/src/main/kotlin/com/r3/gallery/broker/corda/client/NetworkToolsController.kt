package com.r3.gallery.broker.corda.client

import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.api.NetworkBalancesResponse
import com.r3.gallery.api.Participant
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

/**
 * Controller with aggregate access across API layers
 * for x-network queries and generic demo operations.
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "false")
@RequestMapping("/network")
class NetworkToolsController(
    @Autowired private val networkToolsService: NetworkToolsService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(NetworkToolsController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    /**
     * REST endpoint for returning participants across all networks.
     *
     * @param networks optional list of networks to filter on
     */
    @GetMapping("/participants")
    fun participants(
        @RequestParam("networks", required = false) networks: List<String>?
    ): CompletableFuture<ResponseEntity<List<Participant>>> {
        logger.info("Request for all participants")
        return CompletableFuture.supplyAsync {
            asResponse(networkToolsService.participants(networks))
        }
    }

    /**
     * REST endpoint for log returns progressUpdates for Node Level state-machine updates
     *
     * @param index to retrieve a subset of log updates, defaults to returning full set of all updates
     */
    @GetMapping("/log")
    fun log(
        @RequestParam("index", required = false) index: Int?
    ): CompletableFuture<ResponseEntity<List<LogUpdateEntry>>> {
        logger.info("Request for logs")
        networkToolsService.getLogs(index)
        return CompletableFuture.supplyAsync {
            asResponse(networkToolsService.getLogs(index))
        }
    }

    /**
     * REST Get balances across all parties and networks
     */
    @GetMapping("/balance")
    fun balance(): CompletableFuture<ResponseEntity<List<NetworkBalancesResponse>>> {
        logger.info("Request for balance of parties across network")
        return CompletableFuture.supplyAsync {
            val queryResult = networkToolsService.getBalance()
            asResponse(
                    queryResult.entries.let {
                        it.map { x500BalanceMap ->
                            val completableBalancesFutures = x500BalanceMap.value
                            val balances = joinFuturesFromList(completableBalancesFutures)
                                NetworkBalancesResponse(
                                        x500 = x500BalanceMap.key,
                                        partyBalances = balances
                                )
                            }
                    }
            )
        }
    }

    /**
     * Initialise the demo by issuing artwork pieces and funds to the demo parties.
     */
    @GetMapping("/init")
    fun initializeDemo(): CompletableFuture<ResponseEntity<Unit>> {
        logger.info("Request for initial issuance to networks.")
        return CompletableFuture.runAsync {
            val initFutures = networkToolsService.initializeDemo()
            joinFuturesFromList(initFutures, true)
        }.thenApply {
            asResponse(Unit)
        }
    }
}