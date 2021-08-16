package com.r3.gallery.broker.corda.client.api

import com.r3.gallery.states.CordaReference

/**
 * Identity of a party on the tokens network.
 */
typealias TokenParty = String

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