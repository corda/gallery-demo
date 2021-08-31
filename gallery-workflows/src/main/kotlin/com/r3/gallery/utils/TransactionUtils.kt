package com.r3.gallery.utils

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.states.LockState
import net.corda.core.crypto.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction

fun WireTransaction.getDependencies(): Set<SecureHash> {
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
        this.componentGroups.map {
            Pair(it.groupIndex, it.components.mapIndexed { internalIndex, internalIt ->
                componentHash(internalIt, this.privacySalt, it.groupIndex, internalIndex)
            })
        }.toMap()
    }

    val availableComponentHashes = this.componentGroups.map {
        Pair(it.groupIndex, it.components.mapIndexed { internalIndex, internalIt ->
            componentHash(availableComponentNonces[it.groupIndex]!![internalIndex], internalIt)
        })
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
        for (i in 0..this.componentGroups.map { it.groupIndex }.maxOrNull()!!) {
            val root = groupsMerkleRoots[i] ?: SecureHash.allOnesHash
            listOfLeaves.add(root)
        }
        listOfLeaves
    }
    return MerkleTree.getMerkleTree(groupHashes)
}

fun WireTransaction.getLockState(serviceHub: ServiceHub, creator: Party, receiver: Party): LockState {
    val notaryIdentity = serviceHub.identityService.partyFromKey(notary!!.owningKey)
        ?: throw IllegalArgumentException("Unable to retrieve party for notary key: ${notary!!.owningKey}")
    val notaryInfo = serviceHub.networkMapCache.getNodeByLegalIdentity(notary!!)
        ?: throw IllegalArgumentException("Unable to retrieve notaryInfo for notary: $notary")
    val signatureMetadata =
        SignatureMetadata(notaryInfo.platformVersion, Crypto.findSignatureScheme(notary!!.owningKey).schemeNumberID)
    // TODO: should this have same window or not? If there's an expiry on this
    return LockState(
        SignableData(id, signatureMetadata),
        creator,
        receiver,
        notaryIdentity,
        timeWindow!!,
        listOf(receiver, creator)
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
