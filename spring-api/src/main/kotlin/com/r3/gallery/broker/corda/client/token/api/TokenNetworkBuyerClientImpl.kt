package com.r3.gallery.broker.corda.client.token.api

import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.states.ValidatedDraftTransferOfOwnership
import com.r3.gallery.workflows.OfferEncumberedTokensFlow
import com.r3.gallery.workflows.RedeemEncumberedTokensFlow
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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class TokenNetworkBuyerClientImpl : TokenNetworkBuyerClient {

    private lateinit var tokenNetworkBuyerCS: ConnectionService

    @Autowired
    @Qualifier("TokenNetworkBuyerProperties")
    private lateinit var tokenNetworkBuyerProperties: ClientProperties

    // init client and set associated network
    @PostConstruct
    private fun postConstruct() {
        tokenNetworkBuyerCS = ConnectionServiceImpl(tokenNetworkBuyerProperties)
        tokenNetworkBuyerCS.associatedNetwork = network
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkBuyerClientImpl::class.java)

        // TODO: properly setup token networks
        private val network = CordaRPCNetwork.GBP
    }

    override fun issueTokens(buyer: TokenParty, amount: Long, currency: String) {
        logger.info("Starting IssueTokensFlow via $buyer for $amount $currency")
        val buyerParty = tokenNetworkBuyerCS.wellKnownPartyFromName(buyer, buyer)
        tokenNetworkBuyerCS.startFlow(buyer, IssueTokensFlow::class.java, amount, currency, buyerParty)
    }

    override fun transferEncumberedTokens(
        buyer: TokenParty,
        seller: TokenParty,
        amount: Int,
        lockedOn: ValidatedUnsignedArtworkTransferTx
    ): TransactionHash {
        logger.info("Starting OfferEncumberedTokensFlow flow via $buyer with seller: $seller")
        val sellerParty = tokenNetworkBuyerCS.wellKnownPartyFromName(buyer, seller)
        val encumberedAmount = Amount(amount.toLong(), FiatCurrency.getInstance("GBP"))
        val wireTx = SerializedBytes<WireTransaction>(lockedOn.transactionBytes).deserialize()
        val controllingNotary = SerializedBytes<Party>(lockedOn.controllingNotaryBytes).deserialize()
        val signatureMetadata = SerializedBytes<SignatureMetadata>(lockedOn.signatureMetadataBytes).deserialize()
        val verifiedDraftTx = ValidatedDraftTransferOfOwnership(wireTx, controllingNotary, signatureMetadata)
        val tx = tokenNetworkBuyerCS.startFlow(
            buyer,
            OfferEncumberedTokensFlow::class.java,
            sellerParty,
            verifiedDraftTx,
            encumberedAmount
        )
        return tx.toString()
    }

    override fun releaseTokens(buyer: TokenParty, encumberedTokens: TransactionHash): TransactionHash {
        val encumberedTxHash = SecureHash.parse(encumberedTokens)
        val stx = tokenNetworkBuyerCS.startFlow(buyer, RedeemEncumberedTokensFlow::class.java, encumberedTxHash)
        return stx.id.toString()
    }
}