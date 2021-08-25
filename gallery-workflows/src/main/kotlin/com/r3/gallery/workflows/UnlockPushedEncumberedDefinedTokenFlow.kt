package com.r3.corda.lib.tokens.workflows.swaps

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.LockContract
import com.r3.gallery.states.LockState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.finance.contracts.asset.Cash
import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Unlock the token states encumbered by [lockStateAndRef].
 * @property lockStateAndRef the lock state [StateAndRef<LockState>] to unlock.
 * @property requiredSignature the [TransactionSignature] required to unlock the lock state.
 */
@StartableByService
@InitiatingFlow
class UnlockPushedEncumberedDefinedTokenFlow(
        private val lockStateAndRef: StateAndRef<LockState>,
        private val requiredSignature: TransactionSignature) : FlowLogic<Unit>() {

    val txTimeWindowTol = Duration.ofMinutes(5)

    @Suspendable
    override fun call() {
        val encumberedTx = serviceHub.validatedTransactions.getTransaction(lockStateAndRef.ref.txhash)
            ?: throw IllegalArgumentException("Unable to find transaction with id: ${lockStateAndRef.ref.txhash}" +
                    " for lock state: ${lockStateAndRef.state.data}")

        val enrichedStateAndRef: StateAndRef<LockState> = encumberedTx.coreTransaction.outRef(lockStateAndRef.ref.index)
        val inputStateAndRefs = getOurEncumberedTokenStates(encumberedTx)
        val outputStates = inputStateAndRefs.map { it.state.data.copy(owner = ourIdentity) }

        val tx = getTransactionBuilder(encumberedTx.notary!!, enrichedStateAndRef, inputStateAndRefs, outputStates)

        tx.verify(serviceHub)
        val locallySignedTx = serviceHub.signInitialTransaction(tx)

        val sessions = enrichedStateAndRef.state.data.participants
            .filter { it != ourIdentity }
            .map { initiateFlow(it) }

        val signedTransaction = subFlow(FinalityFlow(locallySignedTx, sessions, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }

    /**
     * Return a list of our encumbered [StateAndRef<CBDCToken>] states from [SignedTransaction].
     * @param signedTransaction filter outputs of this transaction.
     * @return list of filtered [StateAndRef<CBDCToken>].
     */
    @Suspendable
    private fun getOurEncumberedTokenStates(signedTransaction: SignedTransaction): List<StateAndRef<Cash.State>> {
        val tokenStates = signedTransaction.coreTransaction.outRefsOfType<Cash.State>()

        return tokenStates.filter {
            val party = serviceHub.identityService.requireWellKnownPartyFromAnonymous(it.state.data.owner)
            party == ourIdentity && it.state.encumbrance != null
        }
    }

    private fun getTransactionBuilder(
        notary: Party,
        lockStateRef: StateAndRef<LockState>,
        inputStateAndRefs: List<StateAndRef<Cash.State>>,
        outputStates: List<Cash.State>): TransactionBuilder {

        val transactionBuilder = TransactionBuilder(notary = notary)
        val additionalKeys = inputStateAndRefs.first().state.data.owner.owningKey
        val keys = inputStateAndRefs.map { it.state.data.owner.owningKey }.distinct()
        inputStateAndRefs.map { transactionBuilder.addInputState(it) }
        outputStates.map { transactionBuilder.addOutputState(it) }
        transactionBuilder.addCommand(Cash.Commands.Move(), keys + additionalKeys)

        transactionBuilder.addInputState(lockStateRef)
        transactionBuilder.addCommand(Command(LockContract.Release(requiredSignature), ourIdentity.owningKey))

        return transactionBuilder
    }

//    @Suspendable
//    private fun TransactionBuilder.attachComplianceReferences(tokenIdentifier: UUID) {
//        val tokenComplianceRef = getTokenComplianceRef(tokenIdentifier)
//        this.addReferenceState(tokenComplianceRef.referenced())
//
//        this.setTimeWindow(TimeWindow.untilOnly(Instant.now().plus(txTimeWindowTol)))
//
//        if (tokenComplianceRef.state.data.requiresMemberAccessChecks()) {
//            val ourKycStateAndRef = serviceHub.vaultService.getSingleKYCReferenceState(ourIdentity, tokenIdentifier)
//                ?: throw FlowException("KYC reference state not found for identity: $ourIdentity, " +
//                        "tokenIdentifier: $tokenIdentifier")
//
//            this.addReferenceState(ourKycStateAndRef.referenced())
//        }
//    }
}

/**
 * Responder flow for [UnlockPushedEncumberedDefinedTokenFlow].
 * Sign and finalise the unlock encumbered state transaction.
 */
@InitiatedBy(UnlockPushedEncumberedDefinedTokenFlow::class)
class UnlockPushedEncumberedDefinedTokenFlowHandler(private val otherSession: FlowSession) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    override fun call(): SignedTransaction? {
        return if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            subFlow(ReceiveFinalityFlow(otherSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        } else null
    }
}