package com.r3.gallery.states

import com.r3.gallery.contracts.AuctionContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import java.util.*

@BelongsToContract(AuctionContract::class)
data class AuctionState(
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier,
    val artworkId: UUID
) : LinearState