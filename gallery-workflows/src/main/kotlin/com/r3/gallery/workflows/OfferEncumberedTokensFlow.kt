package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.gallery.states.LockState
import com.r3.gallery.utils.addMoveTokens
import com.r3.gallery.utils.getLockState
import com.r3.gallery.workflows.internal.CollectSignaturesForComposites
import net.corda.core.contracts.Amount
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.CompositeKey
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.serialize
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount as PartyAndAmount1

// Some assumptions:
// - all tokens in this transaction are self issued
// - all inputs in this transaction are from the same party

@InitiatingFlow
@StartableByRPC
class OfferEncumberedTokensFlow(
    val proposedSwapTx: WireTransaction,
    val proposingParty: Party,
    val encumberedAmount: Amount<TokenType>
) : FlowLogic<StateAndRef<LockState>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): StateAndRef<LockState> {
        val lockState = proposedSwapTx.getLockState(serviceHub, ourIdentity, proposingParty)
        val compositeKey = lockState.getCompositeKey() //  (a @ CN1 + b @ CN2) => (a @ CN1 + b' @ CN1)
        val compositeParty = AnonymousParty(compositeKey)
        serviceHub.identityService.registerKey(compositeKey, ourIdentity)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val txBuilder = try {
            with(TransactionBuilder(notary = notary)) {
                addMoveTokens(
                    this,
                    serviceHub,
                    listOf(PartyAndAmount1(compositeParty, encumberedAmount)),
                    ourIdentity,
                    listOf(proposingParty).map { it.owningKey },
                    lockState
                )
                setTimeWindow(proposedSwapTx.timeWindow!!)
            }
        } catch (e: InsufficientBalanceException) {
            throw FlowException("Offered amount ($encumberedAmount) exceeds balance", e)
        }

        txBuilder.verify(serviceHub)
        var signedTx = serviceHub.signInitialTransaction(txBuilder, listOf(ourIdentity.owningKey))
        SerializedBytes<WireTransaction>(signedTx.tx.serialize().bytes)
        // TODO: discuss wht "will not be needed for X-Network, as no additional signers! - DELETE"
        signedTx = subFlow(CollectSignaturesForComposites(signedTx, listOf(proposingParty)))

        val stx = subFlow(FinalityFlow(signedTx, initiateFlow(proposingParty)))

        return with(stx.tx.outRefsOfType(LockState::class.java).single()) {
            StateAndRef(TransactionState(state.data, state.contract, state.notary), ref)
        }
    }
}

/**
 * Responder flow for [OfferEncumberedTokensFlow].
 * Finalise push token transaction.
 */
@InitiatedBy(OfferEncumberedTokensFlow::class)
class OfferEncumberedTokensFlowHandler2(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val compositeKey = CompositeKey.Builder()
            .addKey(ourIdentity.owningKey, weight = 1)
            .addKey(otherSession.counterparty.owningKey, weight = 1)
            .build(1)

        serviceHub.identityService.registerKey(compositeKey, ourIdentity)

        val ff = subFlow(
            ReceiveFinalityFlow(
                otherSideSession = otherSession,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )

//        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED)
//        //val cash: List<Cash.State> = serviceHub.vaultService.queryBy(Cash.State::class.java, inputCriteria).states.map { it.state.data }
//
//        val stateAndRef = serviceHub.vaultService.queryBy(LockState::class.java, inputCriteria).states.last()
//        val lockState = stateAndRef.state.data as LockState
//        val transactionState = TransactionState(lockState, stateAndRef.state.contract, stateAndRef.state.notary)
//        val lockStateAndRef = StateAndRef(transactionState, stateAndRef.ref)
//
//        val unsignedSwapTx = serviceHub.cacheService().getWireTransactionById(lockState.txHash.txId, this.ourIdentity)
//
//        if (unsignedSwapTx != null) {
//            unlockEncumberedStates(lockStateAndRef, unsignedSwapTx)
//        }
    }
//
//    /**
//     * Unlock encumbered states in transaction given by [StateAndRef<LockState>.ref]. Sign and finalise
//     * [unsignedSwapTx] and use the [LockState.controllingNotary] signature to unlock the states in the encumbered push
//     * transaction referenced by [lockStateAndRef].
//     * @param lockStateAndRef the lock state [StateAndRef<LockState>] to unlock.
//     * @param unsignedSwapTx transaction to sign and finalise.
//     */
//    @Suspendable
//    fun unlockEncumberedStates(lockStateAndRef: StateAndRef<LockState>, unsignedSwapTx: WireTransaction) {
//        val signedTx = subFlow(SignAndFinaliseTxForPush(lockStateAndRef, unsignedSwapTx))
//
//        logger.info("Signed and finalised swap tx with id: ${signedTx.id}")
//
//        val controllingNotary = lockStateAndRef.state.data.controllingNotary
//        val requiredSignature = signedTx.getTransactionSignatureForParty(controllingNotary)
//
//        subFlow(UnlockPushedEncumberedDefinedTokenFlow(lockStateAndRef, requiredSignature))
//    }
}
