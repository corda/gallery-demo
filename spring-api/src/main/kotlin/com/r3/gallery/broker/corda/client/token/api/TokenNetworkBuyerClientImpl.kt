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

@Component
class TokenNetworkBuyerClientImpl(
    @Autowired private val connectionManager: ConnectionManager
) : TokenNetworkBuyerClient {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkBuyerClientImpl::class.java)
    }

    override fun issueTokens(buyer: TokenParty, amount: Long, currency: String) {
        logger.info("Starting IssueTokensFlow via $buyer for $amount $currency")
        val connService = connectionManager.connectToCurrencyNetwork(currency)

        val buyerParty = connService.wellKnownPartyFromName(buyer, buyer)
        connService.startFlow(buyer, IssueTokensFlow::class.java, amount, currency, buyerParty)
    }

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
        return tx.toString()
    }

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
}