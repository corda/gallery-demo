package com.r3.gallery.workflows.webapp

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.workflows.webapp.exceptions.InvalidPartyException
import net.corda.core.contracts.ContractState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder
import java.util.*

/**
 * Converts a string x500 name to Party
 */
fun ServiceHub.artworkPartyToParty(artworkParty: ArtworkParty) : AbstractParty {
    try {
        val x500 = CordaX500Name.parse(artworkParty)
        return networkMapCache.getPeerByLegalName(x500)!!
    } catch (e: Exception) {
        throw InvalidPartyException(party = artworkParty)
    }
}

/**
 * Checks if an artwork identifier exists on network
 */
fun ServiceHub.artworkExists(artworkId: ArtworkId) : Boolean
    = vaultService.queryBy(
        ArtworkState::class.java,
        QueryCriteria.LinearStateQueryCriteria().withUuid(listOf(artworkId))
    ).totalStatesAvailable > 0

fun FlowLogic<*>.firstNotary() : Party =
    serviceHub.networkMapCache.notaryIdentities.first()

/**
 * Generates set of flow sessions for all parties across a transaction builder
 * (removes the initiating party)
 *
 * Returns null if there are no sessions required.
 */
fun FlowLogic<*>.initiateFlowSessions(txBuilder: TransactionBuilder): Set<FlowSession>? {
    val participants: List<AbstractParty> = txBuilder.outputStates().flatMap { it.data.participants }
        .plus(txBuilder.inputStates().flatMap { serviceHub.toStateAndRef<ContractState>(it).state.data.participants })
    return if (participants.isNotEmpty()) { (participants-ourIdentity).map { initiateFlow(it) }.toSet() }
        else null
}