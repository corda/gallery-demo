package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.api.TransactionSignature
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.workflows.RevertEncumberedTokensFlow
import com.r3.gallery.workflows.UnlockEncumberedTokensFlow
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Implementation of [TokenNetworkSellerClient]
 */
@Component
class TokenNetworkSellerClientImpl(
    @Autowired
    private val connectionManager: ConnectionManager
) : TokenNetworkSellerClient {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkSellerClientImpl::class.java)
    }

    /**
     * Claims encumbered tokens related to an accepted bid proposal by providing an encumbered transaction reference
     * and a notarySignature.
     *
     * @param sellerParty the party entitled to claim
     * @param currency of the tokens
     * @param encumberedTokens [TransactionHash] of the encumbrance
     * @param notarySignature proof of action that the seller has satisfied their requirement and is entitled to the tokens.
     * @return [TransactionHash]
     */
    override fun claimTokens(
        sellerParty: TokenParty,
        currency: String,
        encumberedTokens: TransactionHash,
        notarySignature: TransactionSignature
    ): TransactionHash {
        logger.info("Starting UnlockEncumberedTokensFlow flow via $sellerParty")
        val connService = connectionManager.connectToCurrencyNetwork(currency)

        val encumberedTokensTxId = SecureHash.parse(encumberedTokens)
        val requiredSignature =
            SerializedBytes<net.corda.core.crypto.TransactionSignature>(notarySignature.bytes).deserialize()
        val signedTx = connService.startFlow(
            sellerParty,
            UnlockEncumberedTokensFlow::class.java,
            encumberedTokensTxId,
            requiredSignature
        ).returnValue.get(ConnectionServiceImpl.TIMEOUT, TimeUnit.SECONDS)
        return signedTx.id.toString()
    }

    /**
     * Releases tokens which are pending POA/encumbered. This is in the case that the seller accepts another bid or the
     * gallery chooses to no longer sell the art. In these cases bids and associated token encumbrances must be rolled back
     * at the request of seller.
     *
     * @param seller choosing to release the tokens
     * @param currency of the tokens
     * @param encumberedTokens [TransactionHash]
     * @return [TransactionHash]
     */
    override fun releaseTokens(
        seller: TokenParty,
        currency: String,
        encumberedTokens: TransactionHash
    ): TransactionHash {
        val connService = connectionManager.connectToCurrencyNetwork(currency)

        val encumberedTxHash = SecureHash.parse(encumberedTokens)
        val stx = connService.startFlow(seller, RevertEncumberedTokensFlow::class.java, encumberedTxHash)
        return stx.id.toString()
    }
}