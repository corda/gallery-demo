package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.*
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.IdentityService
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import java.security.PublicKey


fun WireTransaction.getDependencies () : Set<SecureHash> {
    return this.inputs.map { it.txhash }.toSet() + this.references.map { it.txhash }.toSet()
}

// This function has been brought across from the reissuance library. It has
// been created to reimplement the merkle tree calculation that is performed by
// the wire transaction class. Implementing it ourselves means that we don't have to
// rely on the implementation detail of the wire transaction (which could in theory use a
// stored value if it wanted to)
@Suspendable
fun WireTransaction.generateWireTransactionMerkleTree(): MerkleTree {
    val availableComponentNonces: Map<Int, List<SecureHash>> by lazy {
        this.componentGroups.map { Pair(it.groupIndex, it.components.mapIndexed {
            internalIndex, internalIt ->
            componentHash(internalIt, this.privacySalt, it.groupIndex, internalIndex) })
        }.toMap()
    }

    val availableComponentHashes = this.componentGroups.map {
        Pair(it.groupIndex, it.components.mapIndexed {
            internalIndex, internalIt ->
            componentHash(availableComponentNonces[it.groupIndex]!![internalIndex], internalIt) })
    }.toMap()

    val groupsMerkleRoots: Map<Int, SecureHash> by lazy {
        availableComponentHashes.map {
            Pair(it.key, MerkleTree.getMerkleTree(it.value).hash)
        }.toMap()
    }
    val groupHashes: List<SecureHash> by lazy {
        val listOfLeaves = mutableListOf<SecureHash>()
        // Even if empty and not used, we should at least send oneHashes for each known
        // or received but unknown (thus, bigger than known ordinal) component groups.
        for (i in 0..this.componentGroups.map { it.groupIndex }.max()!!) {
            val root = groupsMerkleRoots[i] ?: SecureHash.allOnesHash
            listOfLeaves.add(root)
        }
        listOfLeaves
    }
    return MerkleTree.getMerkleTree(groupHashes)
}

/**
 * Return a [SignatureMetadata] instance for the notary on this [WireTransaction].
 * @param serviceHub Corda node services.
 * @return [SignatureMetadata] for the transaction's notary.
 * @throws [IllegalArgumentException] if the notary info cannot be retrieved from the network map cache.
 */
@Suspendable
fun WireTransaction.getSignatureMetadataForNotary(serviceHub: ServiceHub) : SignatureMetadata {
    val notary = this.notary!!
    val notaryInfo = serviceHub.networkMapCache.getNodeByLegalIdentity(notary)
            ?: throw IllegalArgumentException("Unable to retrieve notaryInfo for notary: $notary")

    return SignatureMetadata(
            notaryInfo.platformVersion,
            Crypto.findSignatureScheme(notary.owningKey).schemeNumberID
    )
}

/**
 * Return the [TransactionSignature] by [Party] on this [SignedTransaction].
 * @param party find transaction signature by this [Party].
 * @return [TransactionSignature] representing the party's signature on this transaction.
 * @throws
 */
internal fun SignedTransaction.getTransactionSignatureForParty(party: Party): TransactionSignature {
    return this.sigs.single { it.by == party.owningKey }
}

@CordaSerializable
enum class TransactionRole { PARTICIPANT, OBSERVER }

val LedgerTransaction.participants: List<AbstractParty>
    get() {
        val inputParticipants = inputStates.flatMap(ContractState::participants)
        val outputParticipants = outputStates.flatMap(ContractState::participants)
        return inputParticipants + outputParticipants
    }

@Suspendable
fun List<AbstractParty>.toWellKnownParties(services: ServiceHub): List<Party> {
    return map(services.identityService::requireKnownConfidentialIdentity)
}

// Extension function that has nicer error message than the default one from [IdentityService::requireWellKnownPartyFromAnonymous].
@Suspendable
fun IdentityService.requireKnownConfidentialIdentity(party: AbstractParty): Party {
    return wellKnownPartyFromAnonymous(party)
            ?: throw IllegalArgumentException("Called flow with anonymous party that node doesn't know about. " +
                    "Make sure that RequestConfidentialIdentity flow is called before.")
}


// Needs to deal with confidential identities.
@Suspendable
fun requireSessionsForParticipants(participants: Collection<Party>, sessions: List<FlowSession>) {
    val sessionParties = sessions.map(FlowSession::counterparty)
    require(sessionParties.containsAll(participants)) {
        val missing = participants - sessionParties
        "There should be a flow session for all state participants. Sessions are missing for $missing."
    }
}


@Suspendable
fun LedgerTransaction.ourSigningKeys(services: ServiceHub): List<PublicKey> {
    val signingKeys = commands.flatMap(CommandWithParties<*>::signers)
    return services.keyManagementService.filterMyKeys(signingKeys).toList()
}
