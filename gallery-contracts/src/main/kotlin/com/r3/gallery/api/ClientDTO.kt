package com.r3.gallery.api

import net.corda.core.serialization.CordaSerializable
import java.util.*

/**
 * A connection id for indexing CordaRPCConnection between multiple nodes.
 */
typealias RpcConnectionTarget = String

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
 * String X500 name
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
@CordaSerializable
data class ArtworkOwnership(
    val cordaReference: CordaReference,
    val artworkId: ArtworkId,
    val artworkOwner: ArtworkParty
)

typealias TransactionHash = String

@CordaSerializable
data class TransactionSignature(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is TransactionSignature -> bytes.contentEquals(other.bytes)
        else -> false
    }

    override fun hashCode(): Int = bytes.contentHashCode()
}

@CordaSerializable
data class LockStateRef(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is LockStateRef -> bytes.contentEquals(other.bytes)
        else -> false
    }

    override fun hashCode(): Int = bytes.contentHashCode()
}

@CordaSerializable
data class ProofOfTransferOfOwnership(
    val transactionId: CordaReference,
    val transactionHash: TransactionHash,
    val previousOwnerSignature: TransactionSignature,
    val notarySignature: TransactionSignature
)


@CordaSerializable
data class StateRefAndSignature(
    val encumberedTokens: EncumberedTokens,
    val proofOfTransfer: ProofOfTransferOfOwnership
)

@CordaSerializable
data class UnsignedArtworkTransferTx(val transactionBytes: ByteArray) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is UnsignedArtworkTransferTx -> transactionBytes.contentEquals(other.transactionBytes)
        else -> false
    }

    override fun hashCode(): Int = transactionBytes.contentHashCode()
}

@CordaSerializable
data class VerifiedUnsignedArtworkTransferTx(
    val transactionBytes: ByteArray,
    val controllingNotaryBytes: ByteArray,
    val signatureMetadataBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is VerifiedUnsignedArtworkTransferTx -> {
            transactionBytes.contentEquals(other.transactionBytes) &&
                    controllingNotaryBytes.contentEquals(other.controllingNotaryBytes) &&
                    signatureMetadataBytes.contentEquals(other.signatureMetadataBytes)
        }
        else -> false
    }

    override fun hashCode(): Int {
        return 31 * transactionBytes.contentHashCode() + controllingNotaryBytes.contentHashCode() + signatureMetadataBytes.contentHashCode()
    }
}

@CordaSerializable
data class TokenReleaseData(
    val encumberedTokensTxBytes: ByteArray,
    val requiredSignatureBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TokenReleaseData

        if (!encumberedTokensTxBytes.contentEquals(other.encumberedTokensTxBytes)) return false
        if (!requiredSignatureBytes.contentEquals(other.requiredSignatureBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encumberedTokensTxBytes.contentHashCode()
        result = 31 * result + requiredSignatureBytes.contentHashCode()
        return result
    }
}

@CordaSerializable
data class SignedTokenTransferTx(val transactionBytes: ByteArray) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is SignedTokenTransferTx -> transactionBytes.contentEquals(other.transactionBytes)
        else -> false
    }

    override fun hashCode(): Int = transactionBytes.contentHashCode()
}


@CordaSerializable
data class UnsignedArtworkTransferTxAndLock(val transactionBytes: ByteArray, val lockBytes: ByteArray) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is UnsignedArtworkTransferTxAndLock -> {
            transactionBytes.contentEquals(other.transactionBytes) && lockBytes.contentEquals(other.lockBytes)
        }
        else -> false
    }

    override fun hashCode(): Int {
        return 31 * transactionBytes.contentHashCode() + lockBytes.contentHashCode()
    }
}

//typealias EncumberedTokens = CordaReference
typealias EncumberedTokens = LockStateRef

enum class CordaRPCNetwork(val netName: String) {
    AUCTION("auction"),
    GBP("gbp"),
    CBDC("cbdc")
}