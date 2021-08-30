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
import java.util.*

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
        val wireTransaction = session.sendAndReceive<WireTransaction>(Bid(ourIdentity, artworkId, amount)).unwrap { it }
        return wireTransaction
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



