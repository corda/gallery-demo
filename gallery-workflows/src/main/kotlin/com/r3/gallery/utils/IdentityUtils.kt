package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.IdentityService

@Suspendable
fun List<AbstractParty>.toWellKnownParties(services: ServiceHub): List<Party> {
    return map(services.identityService::requireKnownConfidentialIdentity)
}

// Extension function that has nicer error message than the default one from [IdentityService::requireWellKnownPartyFromAnonymous].
@Suspendable
fun IdentityService.requireKnownConfidentialIdentity(party: AbstractParty): Party {
    return wellKnownPartyFromAnonymous(party)
        ?: throw IllegalArgumentException(
            "Called flow with anonymous party that node doesn't know about. " +
                    "Make sure that RequestConfidentialIdentity flow is called before."
        )
}

// Needs to deal with confidential identities.
@Suspendable
fun requireSessionsForParticipants(participants: Collection<Party>, sessions: List<FlowSession>) {
    val sessionParties = sessions.map(FlowSession::counterparty)
    require(sessionParties.containsAll(participants)) {
        val missing = participants - sessionParties
        "There should be a flow session for all state participants. Sessions are missing for $missing."
    }
}
