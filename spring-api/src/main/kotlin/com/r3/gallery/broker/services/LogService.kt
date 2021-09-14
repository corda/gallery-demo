package com.r3.gallery.broker.services

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.LogUpdateEntry
import net.corda.core.flows.StateMachineRunId
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.StateMachineInfo
import net.corda.core.messaging.StateMachineUpdate
import rx.Subscription

typealias ProgressUpdate = String
typealias ProgressUpdateSubscription = Subscription
typealias LogRetrievalIdx = Int

/**
 * Stores and returns log updates based on a set of rpc proxies
 * TODO: FE poll tracking based on index of updates list?
 */
class LogService(proxiesAndNetwork: List<Pair<CordaRPCOps, CordaRPCNetwork>>) {

    private val stateMachineSubscriptions: MutableList<Subscription> = ArrayList()
    private val progressUpdateSubscriptions:
        MutableList<Triple<StateMachineInfo, CordaRPCNetwork, ProgressUpdateSubscription>> = ArrayList()
    private val progressUpdates: MutableList<Triple<StateMachineInfo, ProgressUpdate, CordaRPCNetwork>> =  ArrayList()

    init {
        // setup subscriptions to state machines and progressUpdates
        proxiesAndNetwork.forEach { (rpc, network) ->
            val subscription = rpc.stateMachinesFeed().updates.subscribe { smUpdate ->
                if (smUpdate is StateMachineUpdate.Added) {
                    val smUpdateInfo = smUpdate.stateMachineInfo

                    smUpdateInfo.progressTrackerStepAndUpdates?.let { pDataFeed ->
                        progressUpdates.add(Triple(smUpdateInfo, pDataFeed.snapshot, network))
                        val puSub = pDataFeed.updates.subscribe { pUpdate ->
                            progressUpdates.add(Triple(smUpdateInfo, pUpdate, network))
                        }
                        progressUpdateSubscriptions.add(Triple(smUpdateInfo, network, puSub))
                    }
                }
                if (smUpdate is StateMachineUpdate.Removed) {
                    removeProgressSubscriptions(smUpdate.id) // remove subscriptions
                }
            }
            stateMachineSubscriptions.add(subscription)
        }
    }

    /**
     * Unsubscribes and removes all progress subscriptions under a StateMachineRunId
     */
    private fun removeProgressSubscriptions(key: StateMachineRunId) {
        progressUpdateSubscriptions.filter { it.first.id == key }.onEach {
            unsub -> unsub.third.unsubscribe()
        }.also {
            progressUpdateSubscriptions.removeAll(it)
        }
    }

    /**
     * Returns the progress updates across the proxies as an index and list from the requested idx.
     * TODO: Add pruning or cleanup to retrieved, or do we want access to historic?
     */
    fun getProgressUpdates(retrievalIdx: LogRetrievalIdx = 0): Pair<LogRetrievalIdx, List<LogUpdateEntry>> {
        val lastIndex = progressUpdates.lastIndex

        // no new updates
        if (retrievalIdx == lastIndex || lastIndex == -1) {
            return Pair(retrievalIdx, emptyList())
        }

        val updatesSubList = progressUpdates.subList(retrievalIdx, lastIndex)

        return Pair(lastIndex, updatesSubList.map { (smInfo, update, network) ->
            LogUpdateEntry(
                associatedFlow = smInfo.flowLogicClassName,
                network = network.name,
                x500 = smInfo.invocationContext.actor?.owningLegalIdentity?.toString() ?: "",
                logRecordId = smInfo.invocationContext.trace.invocationId.value,
                timestamp = smInfo.invocationContext.trace.invocationId.timestamp.toString(),
                message = update
            )
        })
    }
}