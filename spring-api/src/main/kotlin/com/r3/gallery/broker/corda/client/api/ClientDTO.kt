package com.r3.gallery.broker.corda.client.api

import java.util.*

/**
 * A connection id for indexing CordaRPCConnection between multiple nodes
 */
typealias RPCConnectionId = String

/**
 * A unique reference for something in the Corda system, usually a transaction or state id.
 */
typealias CordaReference = UUID

/**
 * Unique identifier for an artwork, used to look up its name, digitised image etc.
 */
typealias ArtworkId = UUID

/**
 * Identity of the party on the art network.
 */
typealias ArtworkParty = String

/**
 * Identity of a party on the tokens network.
 */
typealias TokenParty = String

/**
 * Represents a state on the art network, identified by [cordaReference], which grants ownership
 * of the artwork identified by [artworkId] to the owner identified by [artworkOwner]
 */
data class ArtworkOwnership(
    val cordaReference: CordaReference,
    val artworkId: ArtworkId,
    val artworkOwner: ArtworkParty
)

typealias TransactionHash = String

data class TransactionSignature(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is TransactionSignature -> bytes.contentEquals(other.bytes)
        else -> false
    }

    override fun hashCode(): Int = bytes.contentHashCode()
}

data class ProofOfTransferOfOwnership(
    val transactionId: CordaReference,
    val transactionHash: TransactionHash,
    val previousOwnerSignature: TransactionSignature,
    val notarySignature: TransactionSignature
)

data class UnsignedArtworkTransferTx(val transactionBytes: ByteArray) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is UnsignedArtworkTransferTx -> transactionBytes.contentEquals(other.transactionBytes)
        else -> false
    }

    override fun hashCode(): Int = transactionBytes.contentHashCode()
}

typealias EncumberedTokens = CordaReference

enum class CordaRPCNetwork(val netName: String) {
    AUCTION("auction"),
    GBP("gbp"),
    CBDC("cbdc")
}