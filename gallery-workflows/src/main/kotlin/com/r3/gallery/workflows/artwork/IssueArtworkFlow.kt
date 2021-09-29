package com.r3.gallery.workflows.artwork

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Companion.ARTWORKCONTRACTID
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant

/**
 * Self-issue an artwork with [artworkId] for auction until [expiry].
 * @param artworkId the UUID of the artwork.
 * @param expiry the Instant until which the transaction can be notarised.
 * @param description of the artwork item.
 * @param url of the artwork.
 * @return the [ArtworkState] representing the artwork item.
 */
@StartableByRPC
@InitiatingFlow
class IssueArtworkFlow(
    private val artworkId: ArtworkId,
    private val expiry: Instant,
    private val description: String = "",
    private val url: String = ""
) : FlowLogic<ArtworkState>() {

    @Suppress("ClassName")
    companion object {
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new artwork item.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }
    }

    override val progressTracker = ProgressTracker(
        GENERATING_TRANSACTION,
        VERIFYING_TRANSACTION,
        SIGNING_TRANSACTION,
        FINALISING_TRANSACTION
    )

    @Suspendable
    override fun call(): ArtworkState {

        progressTracker.currentStep = GENERATING_TRANSACTION
        val state = ArtworkState(artworkId, ourIdentity, expiry, description, url)
        val command = Command(ArtworkContract.Commands.Issue(), listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            .withItems(StateAndContract(state, ARTWORKCONTRACTID), command)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        builder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx = serviceHub.signInitialTransaction(builder)

        progressTracker.currentStep = FINALISING_TRANSACTION
        return subFlow(FinalityFlow(stx, emptyList(), FINALISING_TRANSACTION.childProgressTracker())).tx.outputsOfType(
            ArtworkState::class.java
        ).single()
    }
}
