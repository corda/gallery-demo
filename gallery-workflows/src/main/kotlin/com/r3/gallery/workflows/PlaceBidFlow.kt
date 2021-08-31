package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.unwrap

@StartableByRPC
@InitiatingFlow
class PlaceBidFlow(
    val seller: Party,
    val artworkId: UniqueIdentifier,
    val amount: Amount<TokenType>,
) : FlowLogic<WireTransaction>() {

    @CordaSerializable
    data class Bid(val bidder: Party, val artworkId: UniqueIdentifier, val amount: Amount<TokenType>)

    @Suspendable
    override fun call(): WireTransaction {

        val session = initiateFlow(seller)
        // PB1: bidder/cn1 sends a bid to the gallery/cn1 (request draft transfer of ownership)
        return session.sendAndReceive<WireTransaction>(Bid(ourIdentity, artworkId, amount)).unwrap { it }
    }
}

@StartableByRPC
@InitiatedBy(PlaceBidFlow::class)
class PlaceBidFlowHandler(val session: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        val bid = session.receive<PlaceBidFlow.Bid>().unwrap { it }

        // PB2: gallery/cn1 initiates the transfer of ownership tx back to bidder/cn1
        //      - PB2.1: gallery/cn1 build the transfer-of-ownership tx and sends it to bidder/cn1 for inspection
        //      - PB2.2: bidder/cn1 verifies the tx and responds to gallery/cn1 accepting or rejecting the tx
        //      - PB2.3: gallery/cn1 verifies bidder/cn1 accepted, returns the draft tx, throws if bidder/cn1 rejected
        val validatedTx = subFlow(SendDraftTransferOfOwnershipFlow(bid.artworkId, bid.bidder))

        // PB3: gallery/cn1 sends the (validated) transfer-of-ownership tx back to bidder/cn1
        session.send(validatedTx)
    }
}



