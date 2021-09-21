package com.r3.gallery.workflows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.gallery.utils.AuctionCurrency
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

/**
 * Issue an [amount] of tokens with given [currency] for the [receiver] or self.
 */
@InitiatingFlow
@StartableByRPC
class IssueTokensFlow(
    private val amount: Long,
    private val currency: String,
    private val receiver: Party? = null
) : FlowLogic<SignedTransaction>() {

    constructor(amount: Amount<TokenType>, receiver: Party? = null) : this(
        amount.quantity,
        amount.token.tokenIdentifier,
        receiver
    )

    @Suppress("ClassName")
    companion object {
        object GETTING_IDENTITIES : ProgressTracker.Step("Getting ours and recipients identity")
        object PARSING_CURRENCY : ProgressTracker.Step("Parsing targetCurrency to issue")
        object ISSUING_TOKENS : ProgressTracker.Step("Issuing tokens to recipient")
    }

    override val progressTracker = ProgressTracker(
        GETTING_IDENTITIES,
        PARSING_CURRENCY,
        ISSUING_TOKENS
    )

    @Suspendable
    override fun call(): SignedTransaction {

        progressTracker.currentStep = PARSING_CURRENCY
        val currencyTokenType = AuctionCurrency.getInstance(currency)

        val amountOfTarget = Amount(amount, currencyTokenType)

        progressTracker.currentStep = GETTING_IDENTITIES
        val tokenToIssue = amountOfTarget issuedBy ourIdentity heldBy (receiver ?: ourIdentity)

        progressTracker.currentStep = ISSUING_TOKENS
        return subFlow(IssueTokens(tokensToIssue = listOf(tokenToIssue), observers = emptyList()))
    }

}

/**
 * Responder flow for [IssueTokensFlow].
 * Sign and finalise the received token transaction.
 */
@InitiatedBy(IssueTokensFlow::class)
open class IssueTokensFlowHandler(private val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(otherPartySession))
    }
}

