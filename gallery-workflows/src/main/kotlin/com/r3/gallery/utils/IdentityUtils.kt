package com.r3.gallery.utils

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.CompositeKey
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import java.security.PublicKey

@Suspendable
fun ServiceHub.registerCompositeKey(ourParty: Party, otherParty: Party): PublicKey {
    val compositeKey = CompositeKey.Builder()
        .addKey(ourParty.owningKey, weight = 1)
        .addKey(otherParty.owningKey, weight = 1)
        .build(1)

    identityService.registerKey(compositeKey, ourParty)

    return compositeKey
}
