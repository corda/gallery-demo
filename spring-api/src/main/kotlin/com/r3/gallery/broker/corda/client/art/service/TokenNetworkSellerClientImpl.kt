package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
import com.r3.gallery.broker.corda.client.config.ClientProperties
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkSellerClient
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.workflows.CreateDraftTransferOfOwnershipFlow
import com.r3.gallery.workflows.PlaceBidFlow
import com.r3.gallery.workflows.UnlockEncumberedTokensFlow
import com.r3.gallery.workflows.artwork.FindArtworkFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.TransactionSignature
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.utilities.getOrThrow
import net.corda.nodeapi.internal.KeyOwningIdentity.UnmappedIdentity.uuid
import org.jgroups.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class TokenNetworkSellerClientImpl(
    @Autowired
    @Qualifier("TokenNetworkGalleryProperties")
    clientProperties: ClientProperties
) : NodeClient(clientProperties), TokenNetworkSellerClient {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkSellerClientImpl::class.java)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override suspend fun claimTokens(
        sellerParty: TokenParty,
        encumberedTokens: EncumberedTokens,
        proofOfTransfer: ProofOfTransferOfOwnership
    ): CordaReference {
        logger.info("Starting UnlockEncumberedTokensFlow flow via $sellerParty")
        val lockStateRef = SerializedBytes<StateRef>(encumberedTokens.bytes).deserialize()
        val requiredSignature = SerializedBytes<TransactionSignature>(proofOfTransfer.notarySignature.bytes).deserialize()
        val signedTx = sellerParty.network().startFlow(UnlockEncumberedTokensFlow::class.java, lockStateRef, requiredSignature)
        return UUID.randomUUID() as CordaReference // TODO:
    }

    override suspend fun releaseTokens(
        sellerParty: TokenParty,
        buyer: TokenParty,
        encumberedTokens: EncumberedTokens
    ): CordaReference {
        TODO("Not yet implemented")
    }

    // TODO: review for multiple tokentype networks§
    internal fun TokenParty.network() : RPCConnectionId
            = (this + CordaRPCNetwork.GBP.toString())
        .also { idExists(it) } // check validity
}