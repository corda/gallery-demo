package com.r3.gallery.broker.services

import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.workflows.webapp.StatesFromTXFlow
import net.corda.core.flows.StateMachineRunId
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.StateMachineUpdate
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
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
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

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
                // "UnlockEncumberedTokensFlowHandler" may not be null
                // "RedeemEncumberedTokensFlowHandler" may not be null
        )
    }

    private val stateMachineSubscriptions: MutableList<Subscription> = ArrayList()
    private val progressSubscriptions: MutableMap<StateMachineRunId, ProgressUpdateSubscription> = ConcurrentHashMap()
    private val progressUpdates: CopyOnWriteArrayList<LogUpdateEntry> =  CopyOnWriteArrayList()
    private val stateMachineRunIdToFlowName: MutableMap<StateMachineRunId, String> = ConcurrentHashMap()
    var isInitialized: Boolean = false

    /**
     * Creates subscriptions to statemachine observables through RPC proxies.
     *
     * Each update is based on ProgressTracker.Step definitions in the Flow being transitioned through the StateMachine
     * and transformed to a DTO [LogUpdateEntry]
     */
    fun initSubscriptions() {
        if (isInitialized) return // final check for race-condition.

        listOf(connectionManager.auction, connectionManager.cbdc, connectionManager.gbp).forEach {
            val currentNetwork = it.associatedNetwork
            it.allConnections()!!.map { connection -> connection.proxy }.forEach { rpc ->

                val firingX500 = rpc.nodeInfo().legalIdentities.first().name
                val subscription = rpc.stateMachinesFeed().updates.subscribe subscriptionSet@{ smUpdate ->
                    // event for Flow being added to the StateMachine track
                    if (smUpdate is StateMachineUpdate.Added) {
                        try {
                            val smUpdateInfo = smUpdate.stateMachineInfo
                            val flowForUpdate = smUpdateInfo.flowLogicClassName
                            stateMachineRunIdToFlowName[smUpdate.id] = flowForUpdate // track flow name to id

                            if ( // filter out blacklist (polling flows) and only consider targets
                                    flowsToExplicitlyIgnore.any { flowName -> flowName in flowForUpdate } ||
                                    !flowsToTrackProgress.any { flowName -> flowName in flowForUpdate }
                            ) return@subscriptionSet

                            smUpdateInfo.progressTrackerStepAndUpdates?.let { feed ->
                                thread {
                                    val logRecordId = UUID.randomUUID().toString()
                                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(Date.from(Instant.now()))
                                    progressSubscriptions[smUpdateInfo.id] = (feed.updates.subscribe { msg ->
                                        // ignore structural step change updates and add context to Starting and Done messages.
                                        if (!msg.contains("Structural step change")) {
                                            val update_ = if (msg == "Starting" || msg == "Done") { "$msg ${flowForUpdate}."}
                                            else msg
                                            val updateProposal = LogUpdateEntry(
                                                    associatedFlow = flowForUpdate,
                                                    network = currentNetwork.name,
                                                    x500 = firingX500.toString(),
                                                    logRecordId = logRecordId,
                                                    timestamp = sdf,
                                                    message = update_
                                            )
                                            // Avoid duplicates - see LogUpdateEntry.equals
                                            progressUpdates.add(updateProposal)
                                        }
                                    })
                                }
                            }
                        } catch (e: Error) {
                            logger.info("Error in smUpdate Add ${e.message}")
                            throw IllegalStateException("ERROR ADD")
                        }
                    }
                    // event for Flow exiting StateMachine
                    if (smUpdate is StateMachineUpdate.Removed) {
                        try {
                            val associatedFlow = stateMachineRunIdToFlowName[smUpdate.id]
                            val signers: Map<CordaX500Name, Boolean>

                            if ( // filter out blacklist (polling flows) and only consider targets
                                    flowsToExplicitlyIgnore.any { flowName -> flowName in associatedFlow!! } ||
                                    !flowsToTrackProgress.any { flowName -> flowName in associatedFlow!! } ||
                                    flowsToIgnoreCompletionUpdate.any { flowName -> flowName in associatedFlow!! }
                            ) return@subscriptionSet

                            // Cast to correct Object for decomposition to serialized states of 'completed' property
                            // Wire and SignedTransaction are converted to [LedgerTransaction] and ContractStates resolved
                            // via StatesFromTXFlow.
                            var wtx: WireTransaction? = null
                            var stx: SignedTransaction? = null
                            if (
                                    associatedFlow!!.contains("RequestDraftTransferOfOwnershipFlow")
                            ) { // target flows are all SignedTransaction except RequestDraft which is pair
                                wtx = (smUpdate.result.getOrThrow() as Pair<*,*>).first as WireTransaction
                                signers = wtx.requiredSigningKeys.associate { pKey ->
                                    Pair(rpc.partyFromKey(pKey)!!.name, false) // no signatures are applied
                                }
                            } else {
                                stx = smUpdate.result.getOrThrow() as SignedTransaction
                                signers = stx.requiredSigningKeys.associate { pKey ->
                                    val hasSigned: Boolean = !stx.getMissingSigners().contains(pKey)
                                    Pair(rpc.partyFromKey(pKey)!!.name, hasSigned)
                                }
                            }
                            val flowHandle = if (wtx != null) rpc.startFlowDynamic(StatesFromTXFlow::class.java, wtx)
                                    else rpc.startFlowDynamic(StatesFromTXFlow::class.java, stx)
                            flowHandle.returnValue.toCompletableFuture().also { cfState ->
                                val txStates = cfState.get(ConnectionServiceImpl.TIMEOUT, TimeUnit.SECONDS)
                                val logRecordId = UUID.randomUUID().toString()
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(Date.from(Instant.now()))
                                progressUpdates.add(
                                        LogUpdateEntry(
                                                associatedFlow = associatedFlow,
                                                network = currentNetwork.name,
                                                x500 = firingX500.toString(),
                                                logRecordId = logRecordId,
                                                timestamp = sdf,
                                                message = "",
                                                completed = LogUpdateEntry.FlowCompletionLog(
                                                        associatedStage = associatedFlow,
                                                        logRecordId = logRecordId,
                                                        states = txStates,
                                                        signers = signers
                                                )
                                        )
                                )
                            }
                            progressSubscriptions.remove(smUpdate.id) // remove subscription after completion
                            stateMachineRunIdToFlowName.remove(smUpdate.id)
                        } catch (e: Error) {
                            logger.error("Error in smUpdate removal ${e.message}")
                            isInitialized = false
                            // error will kill subscription reset on next log poll.
                            stateMachineSubscriptions.clear()
                            progressSubscriptions.clear()
                        }
                    }
                }
                logger.info("StateMachine Subscription set for $firingX500")
                stateMachineSubscriptions.add(subscription)
            }
        }
        isInitialized = true
    }

    /**
     * Returns the progress updates across the proxies as an index and list from the requested idx.
     * TODO: Add pruning or cleanup to retrieved, or do we want access to historic?
     */
    fun getProgressUpdates(retrievalIdx: LogRetrievalIdx = 0): Pair<LogRetrievalIdx, List<LogUpdateEntry>> {
        val lastIndex = progressUpdates.lastIndex+1

        // no new updates
        if (retrievalIdx == lastIndex || lastIndex == -1) {
            return Pair(retrievalIdx, emptyList())
        }

        val updatesSubList = progressUpdates.subList(retrievalIdx, lastIndex)

        return Pair(lastIndex, updatesSubList)
    }

    /**
     * Clears / resets logs
     */
    fun clearLogs() {
        progressSubscriptions.clear()
        progressUpdates.clear()
    }
}