package com.r3.gallery.states

import com.r3.gallery.contracts.ArtworkContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty

@BelongsToContract(ArtworkContract::class)
data class ArtworkState(override val participants: List<AbstractParty>, override val linearId: UniqueIdentifier) : LinearState