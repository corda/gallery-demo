package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.states.ArtworkOwnership
import net.corda.core.contracts.FungibleAsset
import net.corda.core.flows.*
import net.corda.core.identity.Party

data class Asset(val name: String) {
}

data class SwapProposal(val asset1: Asset, val asset2: Asset) {
}

/**
 * PartyB/CN2 inspects the proposal and decides to proceed with it. In doing so PartyB builds the transaction Tx0 which
 * consumes Asset2 and generates an encumbered version of Asset2. The encumbered Asset2 has PartyB as its participant.
 */
@StartableByRPC
@InitiatingFlow
class DraftFlows(swapProposal: SwapProposal) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {


    }
}


data class TimeLock(val ) {

}