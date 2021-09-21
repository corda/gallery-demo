package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.api.ValidatedUnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.states.ValidatedDraftTransferOfOwnership
import com.r3.gallery.utils.AuctionCurrency
import com.r3.gallery.workflows.OfferEncumberedTokensFlow
import com.r3.gallery.workflows.RevertEncumberedTokensFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.identity.Party
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.WireTransaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException

/**
 * Implementation of [TokenNetworkBuyerClient]
 */
@Component
class TokenNetworkBuyerClientImpl(
    @Autowired private val connectionManager: ConnectionManager
) : TokenNetworkBuyerClient {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkBuyerClientImpl::class.java)
    }

    /**
     * Issue tokens on a given consideration network
     *
     * @param buyer to issue tokens to (for demo purposes tokens are self-issued)
     * @param amount to issue
     * @param currency string representation of the token description
     */
    override fun issueTokens(buyer: TokenParty, amount: Long, currency: String) {
        logger.info("Starting IssueTokensFlow via $buyer for $amount $currency")
        val connService = connectionManager.connectToCurrencyNetwork(currency)

        val buyerParty = connService.wellKnownPartyFromName(buyer, buyer)
        connService.startFlow(buyer, IssueTokensFlow::class.java, amount, currency, buyerParty)
    }

    /**
     * Creates a transaction to encumber tokens against a lock requiring a notary signature on an unsigned artwork transfer
     *
     * @param buyer encumbering the tokens
     * @param seller who will receive the encumbered tokens
     * @param amount of tokens
     * @param currency representing the token type description
     * @param lockedOn [ValidatedUnsignedArtworkTransferTx] to use as a requirement for notary signature
     */
    override fun transferEncumberedTokens(
        buyer: TokenParty,
        seller: TokenParty,
        amount: Long,
        currency: String,
        lockedOn: ValidatedUnsignedArtworkTransferTx
    ): TransactionHash {
        logger.info("Starting OfferEncumberedTokensFlow flow via $buyer with seller: $seller")
        val connService = connectionManager.connectToCurrencyNetwork(currency)

        val sellerParty = connService.wellKnownPartyFromName(buyer, seller)
        val encumberedAmount = Amount(amount, AuctionCurrency.getInstance(currency))
        val wireTx = SerializedBytes<WireTransaction>(lockedOn.transactionBytes).deserialize()
        val controllingNotary = SerializedBytes<Party>(lockedOn.controllingNotaryBytes).deserialize()
        val signatureMetadata = SerializedBytes<SignatureMetadata>(lockedOn.signatureMetadataBytes).deserialize()
        val verifiedDraftTx = ValidatedDraftTransferOfOwnership(wireTx, controllingNotary, signatureMetadata)
        val tx = connService.startFlow(
            buyer,
            OfferEncumberedTokensFlow::class.java,
            sellerParty,
            verifiedDraftTx,
            encumberedAmount
        )
        return tx.id.toString()
    }

    /**
     * A Buyer request for release of encumbered tokens (return to their ownership) in the case that the seller has
     * not fulfilled their Proof-of-action by the auction expiry time-window. At this time the buyer (creator of lock on
     * encumbrance can release the lock and transfer the tokens back to themselves).
     *
     * @param buyer who wishes to release the tokens
     * @param currency of the token
     * @param encumberedTokens [TransactionHash]
     */
    override fun releaseTokens(
        buyer: TokenParty,
        currency: String,
        encumberedTokens: TransactionHash
    ): TransactionHash {
        val connService = connectionManager.connectToCurrencyNetwork(currency)

        val encumberedTxHash = SecureHash.parse(encumberedTokens)
        val stx = connService.startFlow(buyer, RevertEncumberedTokensFlow::class.java, encumberedTxHash)
        return stx.id.toString()
    }

    override fun resolvePartyFromNameAndCurrency(buyer: TokenParty, currency: String): Party {
        return try {
            return connectionManager.cbdc.wellKnownPartyFromName(buyer, buyer)!!
        } catch (e: IllegalArgumentException) {
            logger.info("Not found on cbdc network")
            null
        } ?: return connectionManager.gbp.wellKnownPartyFromName(buyer, buyer)!!
    }
}