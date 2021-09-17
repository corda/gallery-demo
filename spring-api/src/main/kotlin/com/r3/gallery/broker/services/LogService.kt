package com.r3.gallery.broker.services

import com.r3.gallery.api.LogUpdateEntry
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.workflows.webapp.StatesFromTXFlow
import net.corda.core.contracts.ContractState
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
import java.time.Instant
import java.util.*
import kotlin.collections.HashSet

typealias ProgressUpdateSubscription = Subscription
typealias LogRetrievalIdx = Int

/**
 * Stores and returns log updates based on a set of rpc proxies
 * TODO: FE poll tracking based on index of updates list?
 */
@Component
class LogService(@Autowired private val connectionManager: ConnectionManager) {

    companion object {
        val logger: Logger =  LoggerFactory.getLogger(LogService::class.java)
    }

    private val stateMachineSubscriptions: MutableList<Subscription> = ArrayList()
    private val progressSubscriptions: MutableSet<Pair<StateMachineRunId, ProgressUpdateSubscription>> = HashSet()
    private val progressUpdates: MutableList<LogUpdateEntry> =  ArrayList()
    var isInitialized: Boolean = false

    fun initSubscriptions() {
        listOf(connectionManager.auction, connectionManager.cbdc, connectionManager.gbp).forEach {
            val currentNetwork = it.associatedNetwork
            it.allConnections()!!.map { connection -> connection.proxy }.forEach { rpc ->

                val firingX500 = rpc.nodeInfo().legalIdentities.first().name
                val subscription = rpc.stateMachinesFeed().updates.subscribe { smUpdate ->
                    if (smUpdate is StateMachineUpdate.Added) {
                        val smUpdateInfo = smUpdate.stateMachineInfo

                        // add progress update subscription
                        smUpdateInfo.progressTrackerStepAndUpdates?.updates?.subscribe { update ->
                            if (!update.contains("Structural step change")) {
                                val updateProposal = LogUpdateEntry(
                                    associatedFlow = smUpdateInfo.flowLogicClassName,
                                    network = currentNetwork.name,
                                    x500 = firingX500.toString(),
                                    logRecordId = smUpdate.id.toString() + "-" + UUID.randomUUID(),
                                    timestamp = Date.from(Instant.now()).toString(),
                                    message = update
                                )
                                // avoid duplicates
                                if (updateProposal !in progressUpdates) progressUpdates.add(updateProposal)
                            }
                        }.also { pSub ->
                            logger.info("Progress Subscription set for $firingX500-${smUpdateInfo.flowLogicClassName}-${smUpdateInfo.id}")
                            progressSubscriptions.add(Pair(smUpdateInfo.id, pSub!!))
                        }
                    }
                    if (smUpdate is StateMachineUpdate.Removed) {
                        val logRecordId = smUpdate.id.toString()
                        val associatedFlow = fetchFlowLogicClassFromLogRecordId(logRecordId)
                        if ( // filter for target completions
                            associatedFlow.contains("RequestDraftTransferOfOwnershipFlow") ||
                            associatedFlow.contains("OfferEncumberedTokensFlow") ||
                            associatedFlow.contains("SignAndFinalize") ||
                            associatedFlow.contains("UnlockEncumberedTokensFlow") ||
                            associatedFlow.contains("IssueTokensFlow")
                        ) {
                            val signers: Map<CordaX500Name, Boolean>
                            val states: List<ContractState> = if (
                                associatedFlow.contains("RequestDraftTransferOfOwnershipFlow")
                            ) { // target flows are all SignedTransaction except RequestDraft which is pair
                                val wtx = (smUpdate.result.getOrThrow() as Pair<*,*>).first as WireTransaction
                                signers = wtx.requiredSigningKeys.associate { pKey ->
                                    Pair(rpc.partyFromKey(pKey)!!.name, false) // no signatures are applied
                                }
                                rpc.startFlowDynamic(StatesFromTXFlow::class.java, wtx).returnValue.getOrThrow()
                            } else {
                                val stx = smUpdate.result.getOrThrow() as SignedTransaction
                                signers = stx.requiredSigningKeys.associate { pKey ->
                                    val hasSigned: Boolean = !stx.getMissingSigners().contains(pKey)
                                    Pair(rpc.partyFromKey(pKey)!!.name, hasSigned)
                                }
                                rpc.startFlowDynamic(StatesFromTXFlow::class.java, stx).returnValue.getOrThrow()
                            }
                            progressUpdates.add(
                                LogUpdateEntry(
                                    associatedFlow = associatedFlow,
                                    network = currentNetwork.name,
                                    x500 = firingX500.toString(),
                                    logRecordId = logRecordId + "-" + UUID.randomUUID(),
                                    timestamp = Date.from(Instant.now()).toString(),
                                    message = "",
                                    completed = LogUpdateEntry.FlowCompletionLog(
                                        associatedStage = associatedFlow,
                                        logRecordId = logRecordId + "-" + UUID.randomUUID(),
                                        states = states,
                                        signers = signers
                                    )
                                )
                            )
                        }
                        removeProgressSubscriptions() // remove subscriptions
                    }
                }
                logger.info("StateMachine Subscription set for $firingX500")
                stateMachineSubscriptions.add(subscription)
            }
        }
    }

    private fun fetchFlowLogicClassFromLogRecordId(id: String): String {
        return progressUpdates.find { it.logRecordId == id }!!.associatedFlow
    }

    /**
     * Unsubscribes and removes all progress subscriptions under a StateMachineRunId
     */
    private fun removeProgressSubscriptions() {
        progressSubscriptions.removeIf { it.second.isUnsubscribed }
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