package com.r3.gallery.states

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.contracts.ArtworkContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import com.r3.gallery.contracts.ArtworkContract.Commands

@BelongsToContract(ArtworkContract::class)
data class ArtworkState(
    val issuer: AbstractParty,
    override val owner: AbstractParty,
    val artworkId: ArtworkId,
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier // UUID cordaReference
) : OwnableState, LinearState {
    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(Commands.Transfer(), this.copy(owner = newOwner))
    }
}