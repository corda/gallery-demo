package com.r3.gallery.workflows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.workflows.token.BurnTokens
import jdk.nashorn.internal.runtime.regexp.joni.Config.log
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The [CollectSignaturesInitiatingFlow] is used to automate the collection of counterparty signatures for a given
 * transaction. It requires a list of [signers] of type [Party] rather than a list of [FlowSession]s like in the case
 * of its subflow [CollectSignaturesFlow].
 */
@InitiatingFlow
internal class CollectSignaturesInitiatingFlow(
    private val transaction: SignedTransaction,
    private val signers: List<Party>,
    override val progressTracker: ProgressTracker = CollectSignaturesFlow.tracker()
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // create new sessions to signers and trigger the signing responder flow
        val sessions = signers.map { initiateFlow(it) }
        return subFlow(CollectSignaturesFlow(transaction, sessions, progressTracker))
    }
}

/**
 * The responder flow for [CollectSignaturesInitiatingFlow].
 */
@InitiatedBy(CollectSignaturesInitiatingFlow::class)
internal class CollectSignaturesInitiatingFlowHandler(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CollectSignaturesInitiatingFlowHandler::class.java)
    }

    @Suspendable
    override fun call(): SignedTransaction {

        // sign the transaction and nothing else
        return subFlow(object : SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) {
                logger.info("Signing TX : ${ourIdentity.name}")
            }
        })
    }
}
