package com.r3.gallery.utils

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction

/**
 * Get all transaction's dependencies from the transaction's inputs and references.
 */
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
        for (i in 0..componentGroups.maxOf { it.groupIndex }) {
            val root = groupsMerkleRoots[i] ?: allOnesHash
            listOfLeaves.add(root)
        }
        listOfLeaves
    }

    return MerkleTree.getMerkleTree(groupHashes)
}

/**
 * Return the [TransactionSignature] for the notary of this [SignedTransaction]
 * @return [TransactionSignature] representing the party's signature on this transaction.
 */
fun SignedTransaction.getNotaryTransactionSignature(): TransactionSignature {
    return this.sigs.single { it.by == this.notary!!.owningKey }
}
