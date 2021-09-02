//package com.r3.gallery.workflows.webapp
//
//import com.r3.gallery.api.ArtworkId
//import com.r3.gallery.api.ArtworkParty
//import com.r3.gallery.api.CordaReference
//import com.r3.gallery.states.ArtworkState
//import com.r3.gallery.workflows.webapp.exceptions.InvalidPartyException
//import net.corda.core.contracts.ContractState
//import net.corda.core.flows.FlowLogic
//import net.corda.core.flows.FlowSession
//import net.corda.core.identity.AbstractParty
//import net.corda.core.identity.CordaX500Name
//import net.corda.core.identity.Party
//import net.corda.core.node.ServiceHub
//import net.corda.core.node.services.vault.QueryCriteria
//import net.corda.core.transactions.LedgerTransaction
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.transactions.WireTransaction
//
///**
// * Converts a string x500 name to Party
// */
//fun ServiceHub.artworkPartyToParty(artworkParty: ArtworkParty) : AbstractParty {
//    try {
//        val x500 = CordaX500Name.parse(artworkParty)
//        return networkMapCache.getPeerByLegalName(x500)!!
//    } catch (e: Exception) {
//        throw InvalidPartyException(party = artworkParty)
//    }
//}
//
///**
// * Returns the state associated with an artworkId
// */
//fun ServiceHub.artworkIdToState(artworkId: ArtworkId) : ArtworkState? {
//    return vaultService.queryBy(
//        ArtworkState::class.java,
//    ).states.map { it.state.data }
//        .singleOrNull { it.artworkId == artworkId }
//}
//
///**
// * Returns the state associated with a CordaReference
// *
// * @param cordaReference LinearState id
// */
//inline fun <reified T: ContractState> ServiceHub.cordaReferenceToState(cordaReference: CordaReference) : T? {
//    return vaultService.queryBy(
//        T::class.java,
//        QueryCriteria.LinearStateQueryCriteria().withUuid(listOf(cordaReference))
//    ).states.singleOrNull()?.state?.data
//}
//
///**
// * Checks if an artwork identifier exists on network
// */
//fun ServiceHub.artworkExists(artworkId: ArtworkId) : Boolean
//    = artworkIdToState(artworkId) != null
//
//fun FlowLogic<*>.firstNotary() : Party
//    = serviceHub.networkMapCache.notaryIdentities.first()
//
///**
// * Generates set of flow sessions for all parties across a transaction builder
// * (removes the initiating party)
// *
// * Returns null if there are no sessions required.
// */
//fun <T> FlowLogic<*>.initiateFlowSessions(tx: T): Set<FlowSession>? {
//    when (tx) {
//        is LedgerTransaction -> { // base case and return logic
//            val participantsFromStates = tx.outputStates.flatMap { it.participants }
//                .plus(tx.inputStates.flatMap { it.participants })
//            val participants = (participantsFromStates-ourIdentity)
//            return if (participants.isNotEmpty()) participants.map { initiateFlow(it) }.toSet() else null
//        }
//        is WireTransaction -> tx.toLedgerTransaction(serviceHub).also { initiateFlowSessions(it) }
//        is TransactionBuilder -> tx.toLedgerTransaction(serviceHub).also { initiateFlowSessions(it) }
//        is SignedTransaction -> tx.toLedgerTransaction(serviceHub).also { initiateFlowSessions(it) }
//    }
//    throw IllegalArgumentException("Unable to generate Flow Sessions for parameter $tx")
//}