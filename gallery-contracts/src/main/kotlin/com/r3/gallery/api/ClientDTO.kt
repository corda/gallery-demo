package com.r3.gallery.api

import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount
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
 * Log entry templated for UI output.
 */
data class LogUpdateEntry(
    val associatedFlow: String,
    val network: String,
    val x500: String,
    val logRecordId: String,
    val timestamp: String,
    val message: String
)

/**
 * Participant entry - represents a detailed view of a
 * party (ArtworkParty, TokenParty)
 */
data class Participant(
    val displayName: String,
    val networkIds: List<NetworkId>
)  {
    data class NetworkId(
        val network: String,
        val x500: String,
        val publicKey: String
    )
}

/**
 * Represents a balance for an asset class
 */
data class Balance(
    val currencyCode: String,
    val encumberedFunds: Amount<TokenType>,
    val availableFunds: Amount<TokenType>
)

/**
 * AvailableArtworksResponse
 */
data class AvailableArtworksResponse(
    val artworkId: ArtworkId,
    val description: String,
    val url: String,
    val listed: Boolean,
    val bids: List<BidRecord>
) {
    data class BidRecord(
        val cordaReference: CordaReference,
        val bidderPublicKey: String,
        val bidderDisplayName: ArtworkParty,
        val amountAndCurrency: Amount<TokenType>, // will expand to amount, currencyCode
        val notary: ArtworkParty,
        val expiryDate: Date,
        val accepted: Boolean
    )
}

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

data class TransactionSignature(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is TransactionSignature -> bytes.contentEquals(other.bytes)
        else -> false
    }

    override fun hashCode(): Int = bytes.contentHashCode()
}

data class LockStateRef(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is LockStateRef -> bytes.contentEquals(other.bytes)
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

@CordaSerializable
data class UnsignedArtworkTransferTx(val transactionBytes: ByteArray) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is UnsignedArtworkTransferTx -> transactionBytes.contentEquals(other.transactionBytes)
        else -> false
    }

    override fun hashCode(): Int = transactionBytes.contentHashCode()
}

//typealias EncumberedTokens = CordaReference
typealias EncumberedTokens = LockStateRef

enum class CordaRPCNetwork(val netName: String) {
    AUCTION("auction"),
    GBP("gbp"),
    CBDC("cbdc")
}