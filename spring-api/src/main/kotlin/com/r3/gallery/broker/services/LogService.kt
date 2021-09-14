package com.r3.gallery.broker.services

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.LogUpdateEntry
import net.corda.core.flows.StateMachineRunId
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.StateMachineInfo
import net.corda.core.messaging.StateMachineUpdate
import rx.Subscription
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

typealias ProgressUpdateSubscription = Subscription
typealias LogRetrievalIdx = Int

/**
 * Stores and returns log updates based on a set of rpc proxies
 * TODO: FE poll tracking based on index of updates list?
 */
class LogService(proxiesAndNetwork: List<Pair<CordaRPCOps, CordaRPCNetwork>>) {

    private val stateMachineSubscriptions: MutableList<Subscription> = ArrayList()
    private val progressUpdateSubscriptions:
        MutableList<Triple<StateMachineRunId, CordaRPCNetwork, ProgressUpdateSubscription>> = ArrayList()
    private val progressUpdates: MutableList<LogUpdateEntry> =  ArrayList()

    init {
        // setup subscriptions to state machines and progressUpdates
        proxiesAndNetwork.forEach { (rpc, network) ->
            val subscription = rpc.stateMachinesFeed().updates.subscribe { smUpdate ->
                if (smUpdate is StateMachineUpdate.Added) {
                    val firingX500 = rpc.nodeInfo().legalIdentities.first().name
                    val smUpdateInfo = smUpdate.stateMachineInfo

                    smUpdateInfo.progressTrackerStepAndUpdates?.let { pDataFeed ->
                        // snapshot
//                        updateToLogUpdateEntry(firingX500, smUpdateInfo, pDataFeed.snapshot, network)
                        val puSub = pDataFeed.updates.subscribe { pUpdate ->
                            // observable
                            updateToLogUpdateEntry(firingX500, smUpdateInfo, pUpdate, network)
                        }
                        progressUpdateSubscriptions.add(Triple(smUpdateInfo.id, network, puSub))
                    }
                }
                if (smUpdate is StateMachineUpdate.Removed) {
                    removeProgressSubscriptions() // remove subscriptions
                }
            }
            stateMachineSubscriptions.add(subscription)
        }
    }

    /** transforms to LogUpdateEntry */
    private fun updateToLogUpdateEntry(x500: CordaX500Name, smUpdate: StateMachineInfo, update: String, network: CordaRPCNetwork) {
        // filter out structural step changes
        if (!update.contains("Structural step change")) {
            val updateProposal = LogUpdateEntry(
                    associatedFlow = smUpdate.flowLogicClassName,
                    network = network.name,
                    x500 = x500.toString(),
                    logRecordId = smUpdate.id.toString(),
                    timestamp = Date.from(Instant.now()).toString(),
                    update
            )
            if (updateProposal !in progressUpdates) progressUpdates.add(updateProposal)
        }
    }

    /**
     * Unsubscribes and removes all progress subscriptions under a StateMachineRunId
     */
    private fun removeProgressSubscriptions() {
        progressUpdateSubscriptions.filter { it.third.isUnsubscribed }.also {
            progressUpdateSubscriptions.removeAll(it)
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