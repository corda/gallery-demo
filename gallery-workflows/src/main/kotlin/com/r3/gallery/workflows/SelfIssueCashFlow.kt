package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueFlow
import java.util.*

@InitiatingFlow
@StartableByRPC
class SelfIssueCashFlow(val amount: Amount<Currency>) : FlowLogic<Cash.State>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): Cash.State {
        val issueRef = OpaqueBytes.of(0)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cashIssueTransaction = subFlow(CashIssueFlow(amount, issueRef, notary))
        return cashIssueTransaction.stx.tx.outputs.single().data as Cash.State
    }
}