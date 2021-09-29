package com.r3.gallery.utils

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.CompositeKey
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import java.security.PublicKey

/**
 * Create a composite key from two identities, and register it as the local node's identity.
 * @param ourParty the registrant's identity.
 * @param otherParty the other identity to compose the owning key with.
 * @return the [CompositeKey] from the two identities.
 */
@Suspendable
fun ServiceHub.registerCompositeKey(ourParty: Party, otherParty: Party): PublicKey {
    val compositeKey = CompositeKey.Builder()
        .addKey(ourParty.owningKey, weight = 1)
        .addKey(otherParty.owningKey, weight = 1)
        .build(1)

    identityService.registerKey(compositeKey, ourParty)

    return compositeKey
}
