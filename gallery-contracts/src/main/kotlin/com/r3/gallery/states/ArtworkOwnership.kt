package com.r3.gallery.states

import java.util.*

/**
 * A unique reference for something in the Corda system, usually a transaction or state id.
 */
typealias CordaReference = UUID

/**
 * Unique identifier for an artwork, used to look up its name, digitised image etc.
 */
typealias ArtworkId = UUID

/**
 * Identity of the party on the art network.
 */
typealias ArtworkParty = String

/**
 * Represents a state on the art network, identified by [cordaReference], which grants ownership
 * of the artwork identified by [artworkId] to the owner identified by [artworkOwner]
 */
data class ArtworkOwnership(
        val cordaReference: CordaReference,
        val artworkId: ArtworkId,
        val artworkOwner: ArtworkParty
)