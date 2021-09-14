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
        componentGroups.associate {
            it.groupIndex to it.components.mapIndexed { internalIndex, internalIt ->
                digestService.componentHash(
                    internalIt,
                    privacySalt,
                    it.groupIndex,
                    internalIndex
                )
            }
        }
    }

    val availableComponentHashes: Map<Int, List<SecureHash>> by lazy {
        componentGroups.associate {
            it.groupIndex to it.components.mapIndexed { internalIndex, internalIt ->
                digestService.componentHash(
                    availableComponentNonces[it.groupIndex]!![internalIndex],
                    internalIt
                )
            }
        }
    }

    val groupsMerkleRoots: Map<Int, SecureHash> by lazy {
        availableComponentHashes.entries.associate { it.key to MerkleTree.getMerkleTree(it.value, digestService).hash }
    }

    val groupHashes: List<SecureHash> by lazy {
        val listOfLeaves = mutableListOf<SecureHash>()
        val allOnesHash = digestService.allOnesHash
        for (i in 0..componentGroups.map { it.groupIndex }.max()!!) {
            val root = groupsMerkleRoots[i] ?: allOnesHash
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
        notaryIdentity,
        timeWindow!!,
        creator,
        receiver,
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

/**
 * Return the [TransactionSignature] for the notary of this [SignedTransaction]
 * @return [TransactionSignature] representing the party's signature on this transaction.
 * @throws
 */
fun SignedTransaction.getNotaryTransactionSignature(): TransactionSignature {
    return this.sigs.single { it.by == this.notary!!.owningKey }
}
