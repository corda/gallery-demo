package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.gallery.contracts.LockContract
import com.r3.gallery.states.LockState
import com.r3.gallery.utils.addMoveTokens
import net.corda.core.contracts.Command
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * Unlock the token states encumbered by [lockStateAndRef].
 * @property encumberedTxHash the TX ID of the encumbered token offer transaction
 * @property notarySignature the [TransactionSignature] required to unlock the lock state.
 */
@StartableByRPC
@InitiatingFlow
class UnlockEncumberedTokensFlow(
    private val encumberedTxHash: SecureHash,
    private val notarySignature: TransactionSignature
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val encumberedTx = serviceHub.validatedTransactions.getTransaction(encumberedTxHash)
            ?: throw IllegalArgumentException("Unable to find transaction with id: ${encumberedTxHash}")

        val lockStates = encumberedTx.coreTransaction.outRefsOfType<LockState>().single()
        val tokensStates = encumberedTx.coreTransaction.outRefsOfType<FungibleToken>().filter {
            val party = serviceHub.identityService.requireWellKnownPartyFromAnonymous(it.state.data.holder)
            party == ourIdentity && it.state.encumbrance != null
        }

        val compositeKey = tokensStates.first().state.data.holder.owningKey
        val outputStates = tokensStates.map { it.state.data.withNewHolder(ourIdentity) }

        val txBuilder = TransactionBuilder(notary = encumberedTx.notary!!)
            .addMoveTokens(tokensStates, outputStates, listOf(compositeKey))
            .addInputState(lockStates)
            .addCommand(Command(LockContract.Release(notarySignature), ourIdentity.owningKey))

        txBuilder.verify(serviceHub)
        val locallySignedTx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = lockStates.state.data.participants
            .filter { it != ourIdentity }
            .map { initiateFlow(it) }

        return subFlow(FinalityFlow(locallySignedTx, sessions, statesToRecord = StatesToRecord.ALL_VISIBLE))
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