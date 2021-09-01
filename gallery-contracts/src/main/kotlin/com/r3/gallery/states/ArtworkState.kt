package com.r3.gallery.states

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Commands
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty

@BelongsToContract(ArtworkContract::class)
data class ArtworkState(
    val issuer: AbstractParty,
    override val owner: AbstractParty,
    val artworkId: ArtworkId,
    override val participants: List<AbstractParty> = listOf(issuer, owner),
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : OwnableState, LinearState {
    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(Commands.Transfer(), this.copy(owner = newOwner, participants = listOf(this.owner, newOwner)))
    }
}