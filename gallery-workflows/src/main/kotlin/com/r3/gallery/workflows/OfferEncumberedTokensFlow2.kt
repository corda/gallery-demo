package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.types.toPairs
import com.r3.corda.lib.tokens.workflows.utilities.addTokenTypeJar
import com.r3.gallery.contracts.LockContract
import com.r3.gallery.states.LockState
import net.corda.core.contracts.*
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.internal.requiredContractClassName
import net.corda.core.node.ServiceHub
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import java.security.PublicKey
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount as PartyAndAmount1

// assumptions:
// - all tokens in this transaction are self issued
// - all inputs in this transaction are from the same party

@InitiatingFlow
@StartableByRPC
class OfferEncumberedTokensFlow2(
    val proposedSwapTx: WireTransaction,
    val partyToMoveTo: Party,
    val amount: Amount<TokenType>
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val signatureMetadata = proposedSwapTx.getSignatureMetadataForNotary(serviceHub)
        val lockState = getLockState(signatureMetadata)
        val compositeKey = lockState.getCompositeKey() //  (a @ CN1 + b @ CN2) => (a @ CN1 + b' @ CN1)
        val compositeParty = AnonymousParty(compositeKey)
        serviceHub.identityService.registerKey(compositeKey, ourIdentity)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val txBuilder = try {
            with(TransactionBuilder(notary = notary)) {
                addMoveEncumberedTokens(
                    this,
                    serviceHub,
                    listOf(PartyAndAmount1(compositeParty, amount)),
                    ourIdentity,
                    listOf(partyToMoveTo).map { it.owningKey },
                    lockState
                )
                setTimeWindow(proposedSwapTx.timeWindow!!)
            }
        } catch (e: InsufficientBalanceException) {
            throw FlowException("Offered amount ($amount) exceeds balance", e)
        }

        txBuilder.verify(serviceHub)
        var signedTx = serviceHub.signInitialTransaction(txBuilder, listOf(ourIdentity.owningKey))

        // TODO: discuss wht "will not be needed for X-Network, as no additional signers! - DELETE"
        signedTx = subFlow(CollectSignaturesForComposites(signedTx, listOf(partyToMoveTo)))

        return subFlow(FinalityFlow(signedTx, initiateFlow(partyToMoveTo)))
    }


    @Suspendable
    fun addMoveEncumberedTokens(
        transactionBuilder: TransactionBuilder,
        serviceHub: ServiceHub,
        partiesAndAmounts: List<PartyAndAmount1<TokenType>>,
        changeHolder: AbstractParty,
        additionalKeys: List<PublicKey>,
        lockState: LockState,
        queryCriteria: QueryCriteria? = null,
    ): TransactionBuilder {
        val selector = DatabaseTokenSelection(serviceHub)
        val (inputs, outputs) = selector.generateMove(
            partiesAndAmounts.toPairs(),
            changeHolder,
            TokenQueryBy(queryCriteria = queryCriteria),
            transactionBuilder.lockId
        )
        return addMoveEncumberedTokens(
            transactionBuilder = transactionBuilder,
            inputs = inputs,
            outputs = outputs,
            additionalKeys,
            lockState
        )
    }

    @Suspendable
    fun addMoveEncumberedTokens(
        transactionBuilder: TransactionBuilder,
        inputs: List<StateAndRef<AbstractToken>>,
        outputs: List<AbstractToken>,
        additionalKeys: List<PublicKey>,
        lockState: LockState
    ): TransactionBuilder {
        val outputGroups: Map<IssuedTokenType, List<AbstractToken>> = outputs.groupBy { it.issuedTokenType }
        val inputGroups: Map<IssuedTokenType, List<StateAndRef<AbstractToken>>> = inputs.groupBy {
            it.state.data.issuedTokenType
        }

        check(outputGroups.keys == inputGroups.keys) {
            "Input and output token types must correspond to each other when moving tokensToIssue"
        }

        var previousEncumbrance = outputs.size

        transactionBuilder.apply {
            // Add a notary to the transaction.
            notary = inputs.map { it.state.notary }.toSet().single()
            outputGroups.forEach { issuedTokenType: IssuedTokenType, outputStates: List<AbstractToken> ->
                val inputGroup = inputGroups[issuedTokenType]
                    ?: throw IllegalArgumentException("No corresponding inputs for the outputs issued token type: $issuedTokenType")
                val keys = inputGroup.map { it.state.data.holder.owningKey }.distinct()

                var inputStartingIdx = inputStates().size
                var outputStartingIdx = outputStates().size

                val inputIdx = inputGroup.map {
                    addInputState(it)
                    inputStartingIdx++
                }

                val outputIdx = outputStates.map {
                    if (it.holder.owningKey is CompositeKey) {
                        addOutputState(it, it.requiredContractClassName!!, notary!!, previousEncumbrance)
                        previousEncumbrance = outputStartingIdx
                    } else {
                        addOutputState(it)
                    }
                    outputStartingIdx++
                }

                addCommand(
                    MoveTokenCommand(issuedTokenType, inputs = inputIdx, outputs = outputIdx),
                    keys + additionalKeys
                )
            }

            addOutputState(
                state = lockState!!,
                contract = LockContract.contractId,
                notary = notary!!,
                encumbrance = previousEncumbrance
            )

            addCommand(
                LockContract.Encumber(),
                lockState.getCompositeKey()
            )
        }

        addTokenTypeJar(inputs.map { it.state.data } + outputs, transactionBuilder)

        return transactionBuilder
    }

    //
    // TODO: rework - will need to use FungibleTokens instead
    //
    private fun getLockState(signatureMetadata: SignatureMetadata): LockState {
        val notaryIdentity = serviceHub.identityService.partyFromKey(proposedSwapTx.notary!!.owningKey)
        require(notaryIdentity != null) {
            "Unknown notary (key: ${proposedSwapTx.notary!!.owningKey})"
        }

        val lockState = LockState(
            SignableData(proposedSwapTx.id, signatureMetadata),
            ourIdentity,
            partyToMoveTo,
            notaryIdentity!!,
            // TODO: should this be the same window or not? If there's an expiry on this
            proposedSwapTx.timeWindow!!,
            listOf(partyToMoveTo, ourIdentity)
        )
        return lockState
    }
}

/**
 * Responder flow for [OfferEncumberedTokensFlow].
 * Finalise push token transaction.
 */
@InitiatedBy(OfferEncumberedTokensFlow2::class)
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
