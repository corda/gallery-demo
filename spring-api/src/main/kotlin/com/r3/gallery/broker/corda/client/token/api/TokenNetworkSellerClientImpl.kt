package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.api.TransactionSignature
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.workflows.RevertEncumberedTokensFlow
import com.r3.gallery.workflows.UnlockEncumberedTokensFlow
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TokenNetworkSellerClientImpl(
    @Autowired
    private val connectionManager: ConnectionManager
) : TokenNetworkSellerClient {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkSellerClientImpl::class.java)
    }

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
        )
        return signedTx.id.toString()
    }

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