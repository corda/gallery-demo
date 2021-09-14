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
 * - message can/should include formatting tokens for resolving the
 * display on the frontend.
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
 * Participant entry represents a detailed view of a
 * party (ArtworkParty, TokenParty)
 */
data class Participant(
    val displayName: String,
    val x500: String,
    val networkIds: List<NetworkId>,
    val type: AuctionRole
)  {
    // A deconstruct of certificate and key data for a network
    data class NetworkId(
        val network: String,
        val publicKey: String
    )
    // Role across the solution
    enum class AuctionRole { BIDDER, GALLERY }
}

/**
 * Returns balances across network
 */
data class NetworkBalancesResponse(
    val x500: String,
    val partyBalances: List<Balance>
) {
    /**
     * Represents a balance for an asset class
     */
    data class Balance(
        val currencyCode: String,
        val encumberedFunds: Amount<TokenType>,
        val availableFunds: Amount<TokenType>
    )
}

/**
 * Response object for available artwork requests
 */
data class AvailableArtwork(
    val artworkId: ArtworkId,
    val description: String,
    val url: String,
    val listed: Boolean,
    val bids: List<BidRecord>
) {
    // A record of a bid placed against a single artworkId
    data class BidRecord(
        val cordaReference: CordaReference, // UNIQUE
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
    //val transactionId: CordaReference,
    val transactionHash: TransactionHash,
    //val previousOwnerSignature: TransactionSignature,
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

data class ValidatedUnsignedArtworkTransferTx(
    val transactionBytes: ByteArray,
    val controllingNotaryBytes: ByteArray,
    val signatureMetadataBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is ValidatedUnsignedArtworkTransferTx -> {
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

data class TokenReleaseData(
    val encumberedTokens: TransactionHash,
    val notarySignature: TransactionSignature
)

typealias EncumberedTokens = CordaReference

enum class CordaRPCNetwork(val netName: String) {
    AUCTION("auction"),
    GBP("gbp"),
    CBDC("cbdc")
}