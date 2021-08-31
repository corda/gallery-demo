package com.r3.corda.lib.tokens.workflows.swaps

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.workflows.utilities.addTokenTypeJar
import com.r3.gallery.contracts.LockContract
import com.r3.gallery.states.LockState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.lang.IllegalArgumentException
import java.security.PublicKey
import java.time.Duration

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
        val outputStates = inputStateAndRefs.map { it.state.data.withNewHolder(ourIdentity) }

        val tx = getTransactionBuilder(encumberedTx.notary!!, enrichedStateAndRef, inputStateAndRefs, outputStates)

        tx.verify(serviceHub)
        val locallySignedTx = serviceHub.signInitialTransaction(tx)

        val sessions = enrichedStateAndRef.state.data.participants
            .filter { it != ourIdentity }
            .map { initiateFlow(it) }

        val signedTransaction = subFlow(FinalityFlow(locallySignedTx, sessions, statesToRecord = StatesToRecord.ALL_VISIBLE))
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

    @Suspendable
    fun addMoveTokens(
        transactionBuilder: TransactionBuilder,
        inputs: List<StateAndRef<AbstractToken>>,
        outputs: List<AbstractToken>,
        additionalKeys: List<PublicKey>
    ): TransactionBuilder {
        val outputGroups: Map<IssuedTokenType, List<AbstractToken>> = outputs.groupBy { it.issuedTokenType }
        val inputGroups: Map<IssuedTokenType, List<StateAndRef<AbstractToken>>> = inputs.groupBy {
            it.state.data.issuedTokenType
        }

        check(outputGroups.keys == inputGroups.keys) {
            "Input and output token types must correspond to each other when moving tokensToIssue"
        }

        transactionBuilder.apply {
            // Add a notary to the transaction.
            // TODO: Deal with notary change.
            notary = inputs.map { it.state.notary }.toSet().single()
            outputGroups.forEach { issuedTokenType: IssuedTokenType, outputStates: List<AbstractToken> ->
                val inputGroup = inputGroups[issuedTokenType]
                    ?: throw IllegalArgumentException("No corresponding inputs for the outputs issued token type: $issuedTokenType")
                val keys = inputGroup.map { it.state.data.holder.owningKey }

                var inputStartingIdx = inputStates().size
                var outputStartingIdx = outputStates().size

                val inputIdx = inputGroup.map {
                    addInputState(it)
                    inputStartingIdx++
                }

                val outputIdx = outputStates.map {
                    addOutputState(it)
                    outputStartingIdx++
                }

                addCommand(MoveTokenCommand(issuedTokenType, inputs = inputIdx, outputs = outputIdx), keys + additionalKeys)
            }
        }

        addTokenTypeJar(inputs.map { it.state.data } + outputs, transactionBuilder)

        return transactionBuilder
    }
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