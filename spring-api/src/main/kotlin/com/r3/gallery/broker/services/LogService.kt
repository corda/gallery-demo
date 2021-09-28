package com.r3.gallery.broker.services

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.workflows.webapp.ContractStatesFromTxFlow
import net.corda.core.flows.StateMachineRunId
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.StateMachineUpdate
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.getOrThrow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import rx.Subscription
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

typealias ProgressUpdateSubscription = Subscription
typealias LogRetrievalIdx = Int

/**
 * Stores and returns log updates based on a set of rpc proxies
 */
@Component
class LogService(@Autowired private val connectionManager: ConnectionManager) {

    /**
     * Companion object contains whitelisting/blacklisting for use in filtering progress tracker updates to most
     * relevant flows.
     */
    companion object {
        val logger: Logger =  LoggerFactory.getLogger(LogService::class.java)
        val flowsToTrackProgress = listOf(
                "RequestDraftTransferOfOwnershipFlow",
                "RequestDraftTransferOfOwnershipFlowHandler",
                "OfferEncumberedTokensFlow",
                "OfferEncumberedTokensFlowHandler",
                "SignAndFinalizeTransferOfOwnership",
                "SignAndFinaliseTxForPushHandler",
                "UnlockEncumberedTokensFlow",
                "UnlockEncumberedTokensFlowHandler",
                "RevertEncumberedTokensFlow",
                "RedeemEncumberedTokensFlowHandler"
        )
        val flowsToExplicitlyIgnore = listOf(
                "FindOwnedArtworksFlow",
                "GetBalanceFlow"
        )
        val flowsToIgnoreCompletionUpdate = listOf(
                "RequestDraftTransferOfOwnershipFlowHandler",
                "OfferEncumberedTokensFlowHandler",
                "SignAndFinaliseTxForPushHandler",
                "UnlockEncumberedTokensFlowHandler",
                "RedeemEncumberedTokensFlowHandler"
        )
    }

    /**
     * Data class for passing identity information across progress subscriptions and handlers. Useful for composing
     * LogUpdateEntry objects.
     */
    data class ProjectUpdateIdentity(
            val network: CordaRPCNetwork,
            val name: CordaX500Name,
            val initiatingFlow: String,
            val progressUpdateSubscription: ProgressUpdateSubscription
    )

    private var progressCache: MutableList<LogUpdateEntry> = CopyOnWriteArrayList()

    private val networkList: List<ConnectionService> = listOf(connectionManager.auction, connectionManager.cbdc, connectionManager.gbp)
    private val stateMachineSubscriptions: MutableMap<Subscription, Pair<CordaRPCNetwork, CordaX500Name>> = ConcurrentHashMap()
    private val progressSubscriptions: MutableMap<StateMachineRunId, ProjectUpdateIdentity> = ConcurrentHashMap()
    private val progressUpdates: CopyOnWriteArrayList<LogUpdateEntry> =  CopyOnWriteArrayList()

    /**
     * Initializer and subscriber to cross-network state machines. Subscribes to an observable for updates.
     * This function can be called on demand and will re-subscribe to any down subscriptions and leave intact existing
     * subscriptions.
     */
    fun subscribeToRpcConnectionStateMachines() {
        networkList.map { cs -> cs.allProxies()!!.entries }.forEach { connections -> // all connection entries from all networks

            // iterate through all connections on each network setting up subs
            connections.forEach subscriptionStart@ { connection ->
                val cordaX500Name = connection.key
                val network = connection.value.first
                val proxy = connection.value.second

                // check for existing subscription if existing make sure it's unsubscribed or leave it.
                stateMachineSubscriptions.entries.firstOrNull {
                    it.value.first == network && it.value.second == cordaX500Name
                }?.let { entry ->
                    if (!entry.key.isUnsubscribed) return@subscriptionStart // already subscribed to this connection skip
                }

                val subscription = proxy.stateMachinesFeed().updates.subscribe(
                        {
                            stateMachineSubHandler(it, network, cordaX500Name)
                        }
                ) {
                    logger.error("Error with state machine subscription. Reinitialising subscriptions. Error: $it")
                    subscribeToRpcConnectionStateMachines()
                }
                logger.info("Subscribing to state machine $network:$cordaX500Name")
                stateMachineSubscriptions[subscription] = Pair(network, cordaX500Name)
            }
        }
    }

    /**
     * Subscription handler for RPCConnection state machine Observer subscriptions. Creates the progress tracker subscriptions
     * for any flows which are executed on the node of the calling subscriber.
     *
     * @param smUpdate a state machine update
     * @param network network which the update belongs
     * @param cordaX500Name the name of the flow initiator
     */
    private fun stateMachineSubHandler(smUpdate: StateMachineUpdate, network: CordaRPCNetwork, cordaX500Name: CordaX500Name) {
        if (smUpdate is StateMachineUpdate.Added) { // subscribe to progress trackers on add
            val stateMachineInfo = smUpdate.stateMachineInfo
            val flowName = stateMachineInfo.flowLogicClassName

            // Only track flows related to Swaps algorithm
            if (!flowsToTrackProgress.any { flow -> flow in flowName} ||
                    flowsToExplicitlyIgnore.any { flow -> flow in flowName }
            ) return

            // Subscribe to progress trackers
            stateMachineInfo.progressTrackerStepAndUpdates?.updates?.toList()?.subscribe(
                    {   progressUpdateList ->
                        progressTrackerSubHandler(progressUpdateList, flowName, network, cordaX500Name)
                    }
            )
            {
                logger.error("Error with progress tracker subscription for flow $flowName and state-machine ${stateMachineInfo.id}")
            }?.also { subscription -> // store record of subscriptions in flight.
                progressSubscriptions[stateMachineInfo.id] = ProjectUpdateIdentity(network, cordaX500Name, flowName, subscription)
            }
        } else if (smUpdate is StateMachineUpdate.Removed) { // generate final record with complete object serialization on exit from statemachine
            val id = smUpdate.id
            val result = smUpdate.result
            val projectUpdateIdentity = progressSubscriptions[id]

            result.doOnSuccess { resultObject ->
                if (resultObject != null && projectUpdateIdentity != null) {
                    progressTrackerCompleteHandler(
                            resultObject,
                            projectUpdateIdentity.initiatingFlow,
                            projectUpdateIdentity.network,
                            projectUpdateIdentity.name
                    )
                }
            }

            result.doOnFailure {
                logger.error("Error in completion handler of $id related to initiating flow $projectUpdateIdentity")
            }
        }
    }

    /**
     * Constructs a LogUpdateRecord for all progress updates from the entry to the statemachine just prior to exit/completion
     *
     * @param msgList list of all updates in the progression of the flow until last step prior to completion
     * @param flowForUpdate name of flow in flight
     * @param network network which the update belongs
     * @param cordaX500Name the name of the flow initiator
     */
    private fun progressTrackerSubHandler(msgList: List<String>, flowForUpdate: String, network: CordaRPCNetwork, cordaX500Name: CordaX500Name) {
        // transform and add to resultList
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        msgList.forEach { msg ->
            val logRecordId = UUID.randomUUID().toString()
            if (!msg.contains("Structural step change")) {
                val update_ = if (msg == "Starting" || msg == "Done") { "$msg ${flowForUpdate}."}
                else msg

                // create record and add to cached list
                val updateProposal = LogUpdateEntry(
                        associatedFlow = flowForUpdate,
                        network = network.name,
                        x500 = cordaX500Name.toString(),
                        logRecordId = logRecordId,
                        timestamp = sdf.format(Date.from(Instant.now())),
                        message = update_
                )
                progressUpdates.add(updateProposal)
            }
        }
    }

    /**
     * Constructs a LogUpdateRecord from an update which removes/exits a flow from the state machine.
     *
     * @param result the resolved outcome (return) of the flow/state machine
     * @param flowForUpdate name of the flow which completed
     * @param network network which the update belongs
     * @param cordaX500Name the name of the flow initiator
     */
    private fun progressTrackerCompleteHandler(result: Any, flowForUpdate: String, network: CordaRPCNetwork, cordaX500Name: CordaX500Name) {
        // ignore completion updates with no result or result not expected
        if (flowsToIgnoreCompletionUpdate.any { flowName -> flowName in flowForUpdate}) return

        // transform, serialize result, and add to result list
        val logRecordId = UUID.randomUUID().toString()
        val signers: Map<CordaX500Name, Boolean>

        // proxy for the initiating rpc connection.
        val initiatorProxy = connectionManager.getCSbyNetwork(network).proxyForParty(cordaX500Name.toString())

        // Cast to correct Object for decomposition to serialized states of 'completed' property
        // Wire and SignedTransaction are converted to [LedgerTransaction] and ContractStates resolved
        // via StatesFromTXFlow.
        var wtx: WireTransaction? = null
        var stx: SignedTransaction? = null
        when (result) {
            is Pair<*,*> -> {
                wtx = result.first as WireTransaction
                signers = wtx.requiredSigningKeys.associate { pKey ->
                    Pair(initiatorProxy.partyFromKey(pKey)!!.name, false) // no signatures are applied
                }
            }
            is SignedTransaction -> {
                stx = result
                signers = stx.requiredSigningKeys.associate { pKey ->
                    val hasSigned: Boolean = !stx.getMissingSigners().contains(pKey)
                    Pair(initiatorProxy.partyFromKey(pKey)!!.name, hasSigned)
                }
            }
            else -> { throw IllegalStateException("Unexpected result in $this. $result.") }
        }

        // Retrieve full states from each transaction inputs, outputs, references
        val flowHandle = if (wtx != null) initiatorProxy.startFlowDynamic(ContractStatesFromTxFlow::class.java, wtx)
        else initiatorProxy.startFlowDynamic(ContractStatesFromTxFlow::class.java, stx)

        try {
            flowHandle.returnValue.getOrThrow().let { resultStates ->
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(Date.from(Instant.now()))

                // create record and add to cached list
                val updateProposal = LogUpdateEntry(
                        associatedFlow = flowForUpdate,
                        network = network.name,
                        x500 = cordaX500Name.toString(),
                        logRecordId = logRecordId,
                        timestamp = sdf,
                        message = "",
                        completed = LogUpdateEntry.FlowCompletionLog(
                                associatedStage = flowForUpdate,
                                logRecordId = logRecordId,
                                states = resultStates,
                                signers = signers
                        )
                )
                progressUpdates.add(updateProposal)
            }
        } catch (e: Exception) {
            logger.error("Error in resolution of smUpdate removal. Cannot retrieve result states from transaction $result.")
        }
    }

    /**
     * Returns the progress updates across the proxies as an index and list from the requested idx.
     *
     * @param retrievalIdx starting index for log record retrieval
     */
    fun getProgressUpdates(retrievalIdx: LogRetrievalIdx = 0): List<LogUpdateEntry> {
        subscribeToRpcConnectionStateMachines()
        val lastIndex = progressUpdates.lastIndex+1
        if (retrievalIdx == lastIndex || lastIndex == -1) return emptyList() // no activity

        return if (progressCache.isNotEmpty()) progressCache.also {
            updateCache(retrievalIdx, lastIndex)
        } else updateCache(retrievalIdx, lastIndex).let { progressCache }
    }

    private fun updateCache(retrievalIdx: LogRetrievalIdx, lastIndex: LogRetrievalIdx) {
        progressCache.clear()
        progressCache.addAll(progressUpdates.subList(retrievalIdx, lastIndex))
    }

    /**
     * Clears / resets logs in memory
     */
    fun clearLogs() {
        progressSubscriptions.clear()
        progressUpdates.clear()
    }
}