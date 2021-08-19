package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.componentHash
import net.corda.core.transactions.WireTransaction


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