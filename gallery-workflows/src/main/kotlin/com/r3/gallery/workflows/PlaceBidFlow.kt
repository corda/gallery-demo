package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap
import java.util.*

@StartableByRPC
@InitiatingFlow
class PlaceBidFlow(
    val seller: Party,
    val artworkId: UniqueIdentifier,
    val amount: Amount<Currency>,
) : FlowLogic<ByteArray>() {

    data class Bid(val bidder: Party, val artworkId: UniqueIdentifier, val amount: Amount<Currency>)

    @Suspendable
    override fun call(): ByteArray {

        val session = initiateFlow(seller)
        return session.sendAndReceive<ByteArray>(Bid(ourIdentity, artworkId, amount)).unwrap { it }
    }
}

@StartableByRPC
@InitiatedBy(PlaceBidFlow::class)
class PlaceBidFlowHandler(val session: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        val bid = session.receive<PlaceBidFlow.Bid>().unwrap { it }
        val validatedTx = subFlow(SendDraftTransferOfOwnershipFlow(bid.artworkId, bid.bidder))
        session.send(validatedTx)
    }
}



