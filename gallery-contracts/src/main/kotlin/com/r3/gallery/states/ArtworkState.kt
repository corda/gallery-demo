package com.r3.gallery.states

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.contracts.ArtworkContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import java.time.Instant

@BelongsToContract(ArtworkContract::class)
data class ArtworkState(
    val artworkId: ArtworkId,
    val owner: AbstractParty,
    val expiry: Instant,
    val description: String = "",
    val url: String = "",
    override val linearId: UniqueIdentifier = UniqueIdentifier()
): LinearState {
    override val participants: List<AbstractParty> get() = listOf(owner)
    /**
     * Returns a copy of this ArtworkState which has a new owner and is not listed.
     */
    fun withNewOwner(newOwner: AbstractParty): ArtworkState { return copy(owner = newOwner) }
}
