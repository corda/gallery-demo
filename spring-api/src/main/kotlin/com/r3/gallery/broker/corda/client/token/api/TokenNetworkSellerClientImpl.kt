package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
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
        val requiredSignature = SerializedBytes<net.corda.core.crypto.TransactionSignature>(notarySignature.bytes).deserialize()
        val signedTx = connService.startFlow(
            sellerParty,
            UnlockEncumberedTokensFlow::class.java,
            encumberedTokensTxId,
            requiredSignature
        )
        return signedTx.id.toString()
    }

    override fun releaseTokens(
        sellerParty: TokenParty,
        buyer: TokenParty,
        currency: String,
        encumberedTokens: TransactionHash
    ): TransactionHash {
        TODO("Implemented in the TokenNetworkBuyerClientImpl")
    }
}