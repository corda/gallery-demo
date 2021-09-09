package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.gallery.contracts.LockContract
import com.r3.gallery.states.LockState
import com.r3.gallery.utils.addMoveTokens
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Duration

/**
 * Unlock the token states encumbered by [lockStateAndRef].
 * @property lockStateAndRef the lock state [StateAndRef<LockState>] to unlock.
 * @property requiredSignature the [TransactionSignature] required to unlock the lock state.
 */
@StartableByService
@InitiatingFlow
class UnlockEncumberedTokensFlow(
    private val lockStateRef: StateRef,
    private val requiredSignature: TransactionSignature) : FlowLogic<SignedTransaction>() {

    val txTimeWindowTol = Duration.ofMinutes(5)

    @Suspendable
    override fun call() : SignedTransaction {
        val encumberedTx = serviceHub.validatedTransactions.getTransaction(lockStateRef.txhash)
            ?: throw IllegalArgumentException("Unable to find transaction with id: ${lockStateRef.txhash}")

        val enrichedStateAndRef: StateAndRef<LockState> = encumberedTx.coreTransaction.outRef(lockStateRef.index)
        val inputStateAndRefs = getOurEncumberedTokenStates(encumberedTx)
        val outputStates = inputStateAndRefs.map { it.state.data.withNewHolder(ourIdentity) }

        val tx = getTransactionBuilder(encumberedTx.notary!!, enrichedStateAndRef, inputStateAndRefs, outputStates)

        tx.verify(serviceHub)
        val locallySignedTx = serviceHub.signInitialTransaction(tx)

        val sessions = enrichedStateAndRef.state.data.participants
            .filter { it != ourIdentity }
            .map { initiateFlow(it) }

        return subFlow(FinalityFlow(locallySignedTx, sessions, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }

    /**
     * Return a list of our encumbered [StateAndRef<AbstractToken>] states from [SignedTransaction].
     * @param signedTransaction filter outputs of this transaction.
     * @return list of filtered [StateAndRef<AbstractToken>].
     */
    @Suspendable
    private fun getOurEncumberedTokenStates(signedTransaction: SignedTransaction): List<StateAndRef<FungibleToken>> {
        val tokenStates = signedTransaction.coreTransaction.outRefsOfType<FungibleToken>()

        return tokenStates.filter {
            val party = serviceHub.identityService.requireWellKnownPartyFromAnonymous(it.state.data.holder)
            party == ourIdentity && it.state.encumbrance != null
        }
    }

    private fun getTransactionBuilder(
        notary: Party,
        lockStateRef: StateAndRef<LockState>,
        inputStateAndRefs: List<StateAndRef<AbstractToken>>,
        outputStates: List<AbstractToken>): TransactionBuilder {

        val txBuilder = TransactionBuilder(notary = notary)
        val compositeKey = inputStateAndRefs.first().state.data.holder.owningKey
        addMoveTokens(txBuilder, inputStateAndRefs, outputStates, listOf(compositeKey))

        txBuilder.addInputState(lockStateRef)
        txBuilder.addCommand(Command(LockContract.Release(requiredSignature), ourIdentity.owningKey))

        return txBuilder
    }
}

/**
 * Responder flow for [UnlockEncumberedTokensFlow].
 * Sign and finalise the unlock encumbered state transaction.
 */
@InitiatedBy(UnlockEncumberedTokensFlow::class)
class UnlockEncumberedTokensFlowHandler(private val otherSession: FlowSession) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    override fun call(): SignedTransaction? {
        return if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            subFlow(ReceiveFinalityFlow(otherSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        } else null
    }
}