package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.api.TransactionSignature

/**
 * Execute flows against Corda nodes running a Token (GBP or CBDC) application, acting as the seller
 */
interface TokenNetworkSellerClient {

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
    fun claimTokens(
        sellerParty: TokenParty,
        currency: String,
        encumberedTokens: TransactionHash,
        notarySignature: TransactionSignature
    ): TransactionHash

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
    fun releaseTokens(seller: TokenParty, currency: String, encumberedTokens: TransactionHash): TransactionHash

}