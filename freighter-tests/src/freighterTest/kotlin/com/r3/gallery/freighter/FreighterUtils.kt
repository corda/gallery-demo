package com.r3.gallery.freighter

import freighter.deployments.SingleNodeDeployed
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.DEFAULT_PAGE_NUM
import net.corda.core.node.services.vault.DEFAULT_PAGE_SIZE
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria

/**
 * Returns all target [T] ContractStates from a node
 * @param node a deployed node to fetch results from
 */
internal inline fun <reified T : ContractState> lookupVault(node : SingleNodeDeployed) : List<StateAndRef<T>> {
    var nextPageNum = DEFAULT_PAGE_NUM
    val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
    var vaultResult = listOf<StateAndRef<T>>()

    do {
        val lkupresult = node.rpc {
            vaultQueryByWithPagingSpec(
                T::class.java,
                criteria,
                PageSpecification(nextPageNum, DEFAULT_PAGE_SIZE)
            )
        }
        vaultResult = vaultResult + lkupresult.states
        nextPageNum++
    } while((DEFAULT_PAGE_SIZE * (nextPageNum - 1)) <= lkupresult.totalStatesAvailable)

    return vaultResult
}

/**
 * Polls a deployed node for a [T] ContractStates until they are available
 * @param node a deployed node to poll for result
 * @param timeout delay
 */
internal inline fun <reified T : ContractState> pollVault(node : SingleNodeDeployed, timeout : Int = 5) : List<T> {
    var vaultResult = listOf<StateAndRef<T>>()
    var retries = 0
    var delay = 1L
    var count = 0

    do {
        count++

        vaultResult = vaultResult + lookupVault(node)

        if (vaultResult.isEmpty()) {

            Thread.sleep(delay * 1000)
            retries += delay.toInt()

            //backoff capped at 30 secs
            if (count % 5 == 0) {
                delay = minOf(delay*2, 30L)
            }
        }
    } while (vaultResult.isEmpty() && retries < timeout)

    return vaultResult.map { it.state.data }
}