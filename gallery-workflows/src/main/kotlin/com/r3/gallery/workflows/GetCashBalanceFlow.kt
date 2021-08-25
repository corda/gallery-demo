package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.workflows.getCashBalances
import java.util.*

@InitiatingFlow
@StartableByRPC
class GetCashBalanceFlow() : FlowLogic<Map<Currency, Amount<Currency>>>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): Map<Currency, Amount<Currency>> {
        return serviceHub.getCashBalances()
    }
}