package com.r3.gallery.worskflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.StartableByRPC

@InitiatingFlow
@StartableByRPC
class IssueTokensFlow(val amount: Long,
                      val currency: String,
                      var receiver: Party? = null) : FlowLogic<SignedTransaction>() {

    init {
        receiver = receiver ?: ourIdentity
    }

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

        val currencyTokenType = FiatCurrency.getInstance(currency)
        val amountOfTarget = amount of currencyTokenType

        val tokenToIssue = amountOfTarget issuedBy ourIdentity heldBy receiver!!
        return subFlow(IssueTokens(tokensToIssue = listOf(tokenToIssue), observers = emptyList()))
    }

}
@InitiatedBy(IssueTokensFlow::class)
open class IssueTokensResponderFlow(private val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(otherPartySession))
    }
}