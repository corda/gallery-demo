package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.workflows.UnlockEncumberedTokensFlow
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.TransactionSignature
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import org.jgroups.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct


@Component
class TokenNetworkSellerClientImpl : TokenNetworkSellerClient {

    private lateinit var tokenNetworkSellerCS: ConnectionService

    @Autowired
    @Qualifier("TokenNetworkSellerProperties")
    private lateinit var tokenNetworkSellerProperties: ClientProperties

    // init client and set associated network
    @PostConstruct
    private fun postConstruct() {
        tokenNetworkSellerCS = ConnectionServiceImpl(tokenNetworkSellerProperties)
        // TODO: properly setup token networks
        tokenNetworkSellerCS.associatedNetwork = network
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkSellerClientImpl::class.java)
        private val network = CordaRPCNetwork.GBP
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun claimTokens(
        sellerParty: TokenParty,
        encumberedTokens: EncumberedTokens,
        proofOfTransfer: ProofOfTransferOfOwnership
    ): CordaReference {
        logger.info("Starting UnlockEncumberedTokensFlow flow via $sellerParty")
        val lockStateRef = SerializedBytes<StateRef>(encumberedTokens.bytes).deserialize()
        val requiredSignature = SerializedBytes<TransactionSignature>(proofOfTransfer.notarySignature.bytes).deserialize()
        val signedTx = tokenNetworkSellerCS.startFlow(sellerParty, UnlockEncumberedTokensFlow::class.java, lockStateRef, requiredSignature)
        return UUID.randomUUID() as CordaReference // TODO:
    }

    override fun releaseTokens(
        sellerParty: TokenParty,
        buyer: TokenParty,
        encumberedTokens: EncumberedTokens
    ): CordaReference {
        TODO("Not yet implemented")
    }
}