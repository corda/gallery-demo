package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.gallery.api.ArtworkOwnership
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.unwrap

@StartableByRPC
@InitiatingFlow
class PlaceBidFlow(
    val galleryParty: AbstractParty,
    val artworkLinearId: UniqueIdentifier,
    val amount: Amount<TokenType>,
) : FlowLogic<WireTransaction>() {

    constructor(galleryParty: AbstractParty, artworkOwnership: ArtworkOwnership, bidAmount: Long, bidCurrency: String) : this(
        galleryParty,
        UniqueIdentifier.fromString(artworkOwnership.cordaReference.toString()),
        Amount(bidAmount, FiatCurrency.getInstance(bidCurrency))
    )

    @CordaSerializable
    data class Bid(val bidder: Party, val artworkId: UniqueIdentifier, val amount: Amount<TokenType>)

    @Suspendable
    override fun call(): WireTransaction {

        val session = initiateFlow(galleryParty)
        // PB1: bidder/cn1 sends a bid to the gallery/cn1 (request draft transfer of ownership)
        return session.sendAndReceive<WireTransaction>(Bid(ourIdentity, artworkLinearId, amount)).unwrap { it }
    }
}

@StartableByRPC
@InitiatedBy(PlaceBidFlow::class)
class PlaceBidFlowHandler(val session: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        val bid = session.receive<PlaceBidFlow.Bid>().unwrap { it }

        val validatedTx = subFlow(CreateDraftTransferOfOwnershipFlow(bid.artworkId, bid.bidder))

        session.send(validatedTx)
    }
}



