package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Companion.ARTWORK_CONTRACT_ID
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class PlaceBidFlow() : FlowLogic<Unit>() {

    @Suspendable
    override fun call(oldOwner: ArtworkOwnership, newOwner: Party): Unit {

    }
}


