package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.unwrap
import java.util.*

@StartableByRPC
@InitiatingFlow
class AcceptBidFlow(val wireTx: WireTransaction) : FlowLogic<Unit>() {

    @Suspendable
    override fun call(): Unit {

    }
}

@StartableByRPC
@InitiatedBy(AcceptBidFlow::class)
class AcceptBidFlowHandler(val session: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

    }
}



