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
 * Controller for x-network queries and generic demo operations.
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
     * @return list of participants as a future
     */
    @GetMapping("/participants")
    fun participants(
        @RequestParam("networks", required = false) networks: List<String>?
    ): CompletableFuture<ResponseEntity<List<Participant>>> {
        logger.info("Request for all participants")
        return CompletableFuture.supplyAsync {
            networkToolsService.participants(networks)
        }.thenApply {
            asResponse(it)
        }
    }

    /**
     * REST endpoint for log returns progressUpdates for Node Level state-machine updates
     *
     * @param index to retrieve a subset of log updates, defaults to returning full set of all updates
     * @return list of [LogUpdateEntry] as a future
     */
    @GetMapping("/log")
    fun log(
        @RequestParam("index", required = false) index: Int?
    ): CompletableFuture<ResponseEntity<List<LogUpdateEntry>>> {
        logger.info("Request for logs")
        return CompletableFuture.supplyAsync {
            networkToolsService.getLogs(index)
        }.thenApply {
            asResponse(it)
        }
    }

    /**
     * REST Get balances across all parties and networks
     *
     * @return list of [NetworkBalancesResponse] as a future
     */
    @GetMapping("/balance")
    fun balance(): CompletableFuture<ResponseEntity<List<NetworkBalancesResponse>>> {
        logger.info("Request for balance of parties across network")
        return CompletableFuture.supplyAsync {
            networkToolsService.getBalance()
        }.thenApply { balanceList ->
            asResponse(
                balanceList.entries.let {
                    it.map { x500BalanceMap ->
                        val completableBalancesFutures = x500BalanceMap.value
                        val balances = joinFuturesFromList<NetworkBalancesResponse.Balance>(completableBalancesFutures)
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
     *
     * @return [Unit]
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