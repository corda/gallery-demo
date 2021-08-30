package com.r3.gallery.services

import com.r3.corda.lib.tokens.workflows.swaps.SignAndFinaliseTxForPush
import com.r3.corda.lib.tokens.workflows.swaps.UnlockPushedEncumberedDefinedTokenFlow
import com.r3.gallery.states.LockState
import com.r3.gallery.workflows.cacheService
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.TransactionSignature
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.getOrThrow

/**
 * Initiate a vault observable to watch produced [LockState] states and resolve the conditions required to unlock them.
 *  - Query, sign and finalise the transaction referenced by the lock state
 *  - Obtain the signature required to unlock the [LockState]
 * @param serviceHub corda services
 */
//@CordaService
//class TokenSwapUnlockService(serviceHub: AppServiceHub) : AbstractVaultUpdateService<LockState>(serviceHub) {
//    override val configEnableString = "enableTokenSwapUnlockService"
//
//    override val trackedContractStateType = LockState::class.java
//
//    override fun filterUpdates(stateAndRefs: List<StateAndRef<ContractState>>): List<StateAndRef<ContractState>> {
//        val ourIdentities = serviceHub.myInfo.legalIdentities
//        return stateAndRefs.filter { !ourIdentities.contains((it.state.data as LockState).creator) }
//    }
//
//    /**
//     * Process a [LockState] update.
//     * Call the [SignAndFinaliseTxForPush] flow with a [StateAndRef<LockState>] and transaction referenced by
//     * [LockState.txHash].
//     * @param stateAndRef the [ContractState] state update to process.
//     */
//    override fun processUpdate(stateAndRef: StateAndRef<ContractState>) {
//        val lockState = stateAndRef.state.data as LockState
//
//        // convert stateAndRef into a StateAndRef<LockState>
//        val transactionState = TransactionState(lockState, stateAndRef.state.contract, stateAndRef.state.notary)
//        val lockStateAndRef = StateAndRef(transactionState, stateAndRef.ref)
//
//        val unsignedSwapTx = serviceHub.cacheService().getWireTransactionById(lockState.txHash.txId)
//
//        if (unsignedSwapTx != null) {
//            executor!!.submit { unlockEncumberedStates(lockStateAndRef, unsignedSwapTx) }
//        }
//        else {
//            logger.info("Received lock state update for the counter side of the proposed swap tx with id: " +
//                    "${lockState.txHash.txId}")
//        }
//    }
//
//    /**
//     * Unlock encumbered states in transaction given by [StateAndRef<LockState>.ref]. Sign and finalise
//     * [unsignedSwapTx] and use the [LockState.controllingNotary] signature to unlock the states in the encumbered push
//     * transaction referenced by [lockStateAndRef].
//     * @param lockStateAndRef the lock state [StateAndRef<LockState>] to unlock.
//     * @param unsignedSwapTx transaction to sign and finalise.
//     */
//    private fun unlockEncumberedStates(lockStateAndRef: StateAndRef<LockState>, unsignedSwapTx: WireTransaction) {
//        val signedTx = serviceHub.startFlow(
//            SignAndFinaliseTxForPush(lockStateAndRef, unsignedSwapTx)).returnValue.getOrThrow()
//
//        logger.info("Signed and finalised swap tx with id: ${signedTx.id}")
//
//        val controllingNotary = lockStateAndRef.state.data.controllingNotary
//        val requiredSignature = signedTx.getTransactionSignatureForParty(controllingNotary)
//
//        serviceHub.startFlow(
//            UnlockPushedEncumberedDefinedTokenFlow(lockStateAndRef, requiredSignature))
//            .returnValue.getOrThrow()
//    }
//
//    /**
//     * Return the [TransactionSignature] by [Party] on this [SignedTransaction].
//     * @param party find transaction signature by this [Party].
//     * @return [TransactionSignature] representing the party's signature on this transaction.
//     * @throws
//     */
//    private fun SignedTransaction.getTransactionSignatureForParty(party: Party): TransactionSignature {
//        return this.sigs.single { it.by == party.owningKey }
//    }
//}