package com.r3.gallery.broker.services

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.workflows.webapp.StatesFromTXFlow
import net.corda.core.contracts.ContractState
import net.corda.core.flows.StateMachineRunId
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.StateMachineUpdate
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.getOrThrow
import rx.Subscription
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

typealias ProgressUpdateSubscription = Subscription
typealias LogRetrievalIdx = Int

/**
 * Stores and returns log updates based on a set of rpc proxies
 * TODO: FE poll tracking based on index of updates list?
 */
class LogService(proxiesAndNetwork: List<Pair<CordaRPCOps, CordaRPCNetwork>>) {

    private val stateMachineSubscriptions: MutableList<Subscription> = ArrayList()
    private val progressSubscriptions: MutableMap<StateMachineRunId, ProgressUpdateSubscription> = HashMap()
//    private val progressUpdateSubscriptions:
//        MutableList<Triple<StateMachineRunId, CordaRPCNetwork, ProgressUpdateSubscription>> = ArrayList()
    private val progressUpdates: MutableList<LogUpdateEntry> =  ArrayList()

    init {
        // setup subscriptions to state machines and progressUpdates
        proxiesAndNetwork.forEach { (rpc, network) ->
            val firingX500 = rpc.nodeInfo().legalIdentities.first().name
            val subscription = rpc.stateMachinesFeed().updates.subscribe { smUpdate ->
                if (smUpdate is StateMachineUpdate.Added) {
                    val smUpdateInfo = smUpdate.stateMachineInfo

                    // add progress update subscription
                    smUpdateInfo.progressTrackerStepAndUpdates?.updates?.subscribe {
                        if (!it.contains("Structural step change")) {
                            val updateProposal = LogUpdateEntry(
                                associatedFlow = smUpdateInfo.flowLogicClassName,
                                network = network.name,
                                x500 = firingX500.toString(),
                                logRecordId = smUpdate.id.toString(),
                                timestamp = Date.from(Instant.now()).toString(),
                                message = it
                            )
                            // avoid duplicates
                            if (updateProposal !in progressUpdates) progressUpdates.add(updateProposal)
                        }
//                        updateToLogUpdateEntry(firingX500, smUpdate, it, network)
                    }.also { progressSubscriptions.putIfAbsent(smUpdateInfo.id, it!!) }

//                    smUpdateInfo.progressTrackerStepAndUpdates?.let { pDataFeed ->
//                        // snapshot
//                        val puSub = pDataFeed.updates.subscribe { pUpdate ->
//                            // observable
//                            updateToLogUpdateEntry(firingX500, smUpdateInfo, pUpdate, network)
//                        }
//                        progressUpdateSubscriptions.add(Triple(smUpdateInfo.id, network, puSub))
//                        progressSubscriptions.putIfAbsent()
//                    }
                }
                if (smUpdate is StateMachineUpdate.Removed) {
                    val logRecordId = smUpdate.id.toString()
                    val associatedFlow = fetchFlowLogicClassFromLogRecordId(logRecordId)
                    if ( // filter for target completions
                        associatedFlow.contains("RequestDraftTransferOfOwnershipFlow") ||
                        associatedFlow.contains("OfferEncumberedTokensFlow") ||
                        associatedFlow.contains("SignAndFinalizeTransferOfOwnership") ||
                        associatedFlow.contains("UnlockEncumberedTokensFlow") ||
                        associatedFlow.contains("IssueTokensFlow")
                    ) {
                        val signers: Map<CordaX500Name, Boolean>
                        val states: List<ContractState> = if (
                            associatedFlow.contains("RequestDraftTransferOfOwnershipFlow")
                        ) { // target flows are all SignedTransaction except RequestDraft which is pair
                            val wtx = (smUpdate.result.getOrThrow() as Pair<*,*>).first as WireTransaction
                            signers = wtx.requiredSigningKeys.associate {
                                Pair(rpc.partyFromKey(it)!!.name, false) // no signatures are applied
                            }
                            rpc.startFlowDynamic(StatesFromTXFlow::class.java, wtx).returnValue.getOrThrow()
                        } else {
                            val stx = smUpdate.result.getOrThrow() as SignedTransaction
                            signers = stx.requiredSigningKeys.associate {
                                val hasSigned: Boolean = stx.getMissingSigners().contains(it)
                                Pair(rpc.partyFromKey(it)!!.name, hasSigned)
                            }
                            rpc.startFlowDynamic(StatesFromTXFlow::class.java, stx).returnValue.getOrThrow()
                        }
                        progressUpdates.add(
                            LogUpdateEntry(
                                associatedFlow = associatedFlow,
                                network = network.name,
                                x500 = firingX500.toString(),
                                logRecordId = logRecordId,
                                timestamp = Date.from(Instant.now()).toString(),
                                message = "",
                                completed = LogUpdateEntry.FlowCompletionLog(
                                    associatedStage = associatedFlow,
                                    logRecordId = logRecordId,
                                    states = states,
                                    signers = signers
                                )
                            )
                        )
                    }
                    removeProgressSubscriptions() // remove subscriptions
                }
            }
            stateMachineSubscriptions.add(subscription)
        }
    }

//    /** transforms to LogUpdateEntry */
//    private fun updateToLogUpdateEntry(x500: CordaX500Name, smUpdate: StateMachineUpdate, message: String?, network: CordaRPCNetwork) {
//        if (smUpdate != null && update is String) { // progressUpdate
//            // filter out structural step changes
//            if (!update.contains("Structural step change")) {
//                val updateProposal = LogUpdateEntry(
//                    associatedFlow = smUpdate.flowLogicClassName,
//                    network = network.name,
//                    x500 = x500.toString(),
//                    logRecordId = smUpdate.id.toString(),
//                    timestamp = Date.from(Instant.now()).toString(),
//                    message = update
//                )
//                // avoid duplicates
//                if (updateProposal !in progressUpdates) progressUpdates.add(updateProposal)
//            }
//        } else { // flow completed update
//            val stx = update as SignedTransaction
//            progressUpdates.add(
//                LogUpdateEntry(
//                    associatedFlow = fetchFlowLogicClassFromLogRecordId(),
//                    network = network.name,
//                    x500 = x500.toString(),
//                    logRecordId = smUpdate.id.toString(),
//                    timestamp = Date.from(Instant.now()).toString(),
//                    message = "",
//                    completed = LogUpdateEntry.FlowCompletionLog(
//
//                    )
//                )
//            )
//        }
//    }

    private fun fetchFlowLogicClassFromLogRecordId(id: String): String {
        return progressUpdates.find { it.logRecordId == id }!!.associatedFlow
    }

    /**
     * Unsubscribes and removes all progress subscriptions under a StateMachineRunId
     */
    private fun removeProgressSubscriptions() {
//        progressUpdateSubscriptions.filter { it.third.isUnsubscribed }.also {
//            progressUpdateSubscriptions.removeAll(it)
//        }
        progressSubscriptions.filterValues { it.isUnsubscribed }.keys.forEach {
            progressSubscriptions.remove(it)
        }
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
}