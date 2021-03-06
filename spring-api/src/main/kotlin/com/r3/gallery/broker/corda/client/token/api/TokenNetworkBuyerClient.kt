package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.*
import net.corda.core.concurrent.CordaFuture
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

/**
 * Execute flows against Corda nodes running a Token (GBP or CBDC) application, acting as the buyer
 */
interface TokenNetworkBuyerClient {

    /**
     * Issue tokens on a given consideration network
     *
     * @param buyer to issue tokens to (for demo purposes tokens are self-issued)
     * @param amount to issue
     * @param currency string representation of the token description
     * @return [CordaFuture][SignedTransaction]
     */
    fun issueTokens(buyer: TokenParty, amount: Long, currency: String = "GBP"): CordaFuture<SignedTransaction>

    /**
     * Creates a transaction to encumber tokens against a lock requiring a notary signature on an unsigned artwork transfer
     *
     * @param buyer encumbering the tokens
     * @param seller who will receive the encumbered tokens
     * @param amount of tokens
     * @param currency representing the token type description
     * @param lockedOn [ValidatedUnsignedArtworkTransferTx] to use as a requirement for notary signature
     * @return [TransactionHash]
     */
    fun transferEncumberedTokens(buyer: TokenParty,
                                 seller: TokenParty,
                                 amount: Long,
                                 currency: String,
                                 lockedOn: ValidatedUnsignedArtworkTransferTx): TransactionHash

    /**
     * A Buyer request for release of encumbered tokens (return to their ownership) in the case that the seller has
     * not fulfilled their Proof-of-action by the auction expiry time-window. At this time the buyer (creator of lock on
     * encumbrance can release the lock and transfer the tokens back to themselves).
     *
     * @param buyer who wishes to release the tokens
     * @param currency of the token
     * @param encumberedTokens [TransactionHash]
     * @return [TransactionHash]
     */
    fun releaseTokens(buyer: TokenParty, currency: String, encumberedTokens: TransactionHash): TransactionHash

    /**
     * Returns a [Party] given a buyer.
     *
     * @param buyer x500 name
     * @return [Party]
     */
    fun resolvePartyFromNameAndCurrency(buyer: TokenParty /* = kotlin.String */, currency: String): Party
}