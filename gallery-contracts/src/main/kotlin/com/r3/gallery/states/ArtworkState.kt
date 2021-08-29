package com.r3.gallery.states

import com.r3.gallery.contracts.ArtworkContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(ArtworkContract::class)
data class ArtworkState(
    val description: String,
    val url: String,
    val owner: Party,
    val listed: Boolean = false,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
): LinearState {
    override val participants: List<AbstractParty> get() = listOf(owner)

    /**
     * Returns a copy of this ArtworkState which has a new owner and is not listed.
     */
    fun transferOwnershipTo(newOwner: Party): ArtworkState { return copy(owner = newOwner, listed = false) }

    /**
     * Returns a copy of this ArtworkState which is not listed.
     */
    fun delist(): ArtworkState { return copy(listed = false) }

    /**
     * Returns a copy of this ArtworkState which is listed.
     */
    fun list(): ArtworkState { return copy(listed = true) }
}
