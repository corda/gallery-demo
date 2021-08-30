package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.LockContract
import com.r3.gallery.states.LockState
import net.corda.core.contracts.*
import net.corda.core.contracts.Amount.Companion.sumOrThrow
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.identity.PartyAndCertificate
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.asset.PartyAndAmount
import net.corda.finance.workflows.asset.selection.AbstractCashSelection
import net.corda.finance.workflows.getCashBalance
import java.security.PublicKey
import java.util.*

@InitiatingFlow
@StartableByRPC
class OfferEncumberedTokensFlow(val proposedSwapTx: WireTransaction, val partyToMoveTo: Party, val amount: Amount<Currency>) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val signatureMetadata = proposedSwapTx.getSignatureMetadataForNotary(serviceHub)
        val lockState = getLockState(signatureMetadata)
        // build composite key ( Proposed | Responder )
        val compositeKey = lockState.getCompositeKey()
        val anonymousParty = AnonymousParty(compositeKey)

        serviceHub.identityService.registerKey(compositeKey, ourIdentity)
//        val wellKnownReceivingParty = serviceHub.identityService.wellKnownPartyFromAnonymous(anonymousParty)
//                ?: throw FlowException("Could not find well known party for $partyToMoveTo")

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txBuilder = TransactionBuilder(notary = notary)

        try {
            generateSpend(serviceHub, txBuilder, listOf(PartyAndAmount(anonymousParty, amount)), ourIdentityAndCert)
        } catch (e: InsufficientBalanceException) {
            throw FlowException("Offered amount ($amount) exceeds balance (${serviceHub.getCashBalance(amount.token)})", e)
        }

        txBuilder.addOutputState(state = lockState!!, contract = LockContract.contractId, notary = notary!!, encumbrance = 0)
        txBuilder.addCommand(LockContract.Encumber(), compositeKey)
        txBuilder.setTimeWindow(proposedSwapTx.timeWindow!!)

        try {
            txBuilder.verify(serviceHub)
        } catch(e: Exception) {
            logger.error("$e")
        }
        var signedTx = serviceHub.signInitialTransaction(txBuilder, listOf(ourIdentity.owningKey))

        // TODO: discuss wht "will not be needed for X-Network, as no additional signers! - DELETE"
        signedTx = subFlow(CollectSignaturesForComposites(signedTx, listOf(partyToMoveTo) /*additionalSigners */ /* plain receiving party because one side of cash is issuer */))

        return subFlow(FinalityFlow(signedTx, initiateFlow(partyToMoveTo)))
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

    @Throws(InsufficientBalanceException::class)
    @Suspendable
    fun generateSpend(services: ServiceHub,
                      tx: TransactionBuilder,
                      payments: List<PartyAndAmount<Currency>>,
                      ourIdentity: PartyAndCertificate,
                      onlyFromParties: Set<AbstractParty> = emptySet(),
                      anonymous: Boolean = true): Triple<TransactionBuilder, List<PublicKey>, List<Cash.State>> {
        var encumbranceIndex = tx.outputStates().size
        fun deriveState(txState: TransactionState<Cash.State>, amt: Amount<Issued<Currency>>, owner: AbstractParty): TransactionState<Cash.State> {
            // TODO: hackery - will replace Cash with FungibleTokens
            return TransactionState(txState.data.copy(amount = amt, owner = owner), encumbrance = ++encumbranceIndex, notary = tx.notary!!)
            //return txState.copy(data = txState.data.copy(amount = amt, owner = owner))
        }

        // Retrieve unspent and unlocked cash states that meet our spending criteria.
        val totalAmount = payments.map { it.amount }.sumOrThrow()
        val cashSelection = AbstractCashSelection.getInstance { services.jdbcSession().metaData }
        val acceptableCoins = cashSelection.unconsumedCashStatesForSpending(services, totalAmount, onlyFromParties, tx.notary, tx.lockId)
        val revocationEnabled = false // Revocation is currently unsupported
        // If anonymous is true, generate a new identity that change will be sent to for confidentiality purposes. This means that a
        // third party with a copy of the transaction (such as the notary) cannot identify who the change was
        // sent to
        val changeIdentity: AbstractParty = if (anonymous) services.keyManagementService.freshKeyAndCert(ourIdentity, revocationEnabled).party.anonymise() else ourIdentity.party
        return internalGenerateSpend(
                tx,
                payments,
                acceptableCoins,
                changeIdentity,
                ::deriveState,
                Cash()::generateMoveCommand
        )
    }

    /**
     * Adds to the given transaction states that move amounts of a fungible asset to the given parties, using only
     * the provided acceptable input states to find a solution (not all of them may be used in the end). A change
     * output will be generated if the state amounts don't exactly fit.
     *
     * The fungible assets must all be of the same type and the amounts must be summable i.e. amounts of the same
     * token.
     *
     * @param tx A builder, which may contain inputs, outputs and commands already. The relevant components needed
     *           to move the cash will be added on top.
     * @param acceptableStates a list of acceptable input states to use.
     * @param payChangeTo party to pay any change to; this is normally a confidential identity of the calling
     * party. We use a new confidential identity here so that the recipient is not identifiable.
     * @param deriveState a function to derive an output state based on an input state, amount for the output
     * and public key to pay to.
     * @param T A type representing a token
     * @param S A fungible asset state type
     * @return A [Pair] of the same transaction builder passed in as [tx], and the list of keys that need to sign
     *         the resulting transaction for it to be valid.
     * @throws InsufficientBalanceException when a cash spending transaction fails because
     *         there is insufficient quantity for a given currency (and optionally set of Issuer Parties).
     */
    @Throws(InsufficientBalanceException::class)
    fun <S : FungibleAsset<T>, T : Any>  internalGenerateSpend(tx: TransactionBuilder,
                                                               payments: List<PartyAndAmount<T>>,
                                                               acceptableStates: List<StateAndRef<S>>,
                                                               payChangeTo: AbstractParty,
                                                               deriveState: (TransactionState<S>, Amount<Issued<T>>, AbstractParty) -> TransactionState<S>,
                                                               generateMoveCommand: () -> CommandData): Triple<TransactionBuilder, List<PublicKey>, List<S>> {
        // Discussion
        //
        // This code is analogous to the Wallet.send() set of methods in bitcoinj, and has the same general outline.
        //
        // First we must select a set of asset states (which for convenience we will call 'coins' here, as in bitcoinj).
        // The input states can be considered our "vault", and may consist of different products, and with different
        // issuers and deposits.
        //
        // Coin selection is a complex problem all by itself and many different approaches can be used. It is easily
        // possible for different actors to use different algorithms and approaches that, for example, compete on
        // privacy vs efficiency (number of states created). Some spends may be artificial just for the purposes of
        // obfuscation and so on.
        //
        // Having selected input states of the correct asset, we must craft output states for the amount we're sending and
        // the "change", which goes back to us. The change is required to make the amounts balance. We may need more
        // than one change output in order to avoid merging assets from different deposits. The point of this design
        // is to ensure that ledger entries are immutable and globally identifiable.
        //
        // Finally, we add the states to the provided partial transaction.

        // TODO: We should be prepared to produce multiple transactions spending inputs from
        // different notaries, or at least group states by notary and take the set with the
        // highest total value.

        // TODO: Check that re-running this on the same transaction multiple times does the right thing.

        // The notary may be associated with a locked state only.
        tx.notary = acceptableStates.firstOrNull()?.state?.notary

        // Calculate the total amount we're sending (they must be all of a compatible token).
        val totalSendAmount = payments.map { it.amount }.sumOrThrow()
        // Select a subset of the available states we were given that sums up to >= totalSendAmount.
        val (gathered, gatheredAmount) = gatherCoins(acceptableStates, totalSendAmount)
        check(gatheredAmount >= totalSendAmount)
        val keysUsed = gathered.map { it.state.data.owner.owningKey }

        // Now calculate the output states. This is complicated by the fact that a single payment may require
        // multiple output states, due to the need to keep states separated by issuer. We start by figuring out
        // how much we've gathered for each issuer: this map will keep track of how much we've used from each
        // as we work our way through the payments.
        val statesGroupedByIssuer = gathered.groupBy { it.state.data.amount.token }
        val remainingFromEachIssuer = statesGroupedByIssuer
                .mapValues {
                    it.value.map {
                        it.state.data.amount
                    }.sumOrThrow()
                }.toList().toMutableList()
        val outputStates = mutableListOf<TransactionState<S>>()
        for ((party, paymentAmount) in payments) {
            var remainingToPay = paymentAmount.quantity
            while (remainingToPay > 0) {
                val (token, remainingFromCurrentIssuer) = remainingFromEachIssuer.last()
                val templateState = statesGroupedByIssuer[token]!!.first().state
                val delta = remainingFromCurrentIssuer.quantity - remainingToPay
                when {
                    delta > 0 -> {
                        // The states from the current issuer more than covers this payment.
                        outputStates += deriveState(templateState, Amount(remainingToPay, token), party)
                        remainingFromEachIssuer[remainingFromEachIssuer.lastIndex] = Pair(token, Amount(delta, token))
                        remainingToPay = 0
                    }
                    delta == 0L -> {
                        // The states from the current issuer exactly covers this payment.
                        outputStates += deriveState(templateState, Amount(remainingToPay, token), party)
                        remainingFromEachIssuer.removeAt(remainingFromEachIssuer.lastIndex)
                        remainingToPay = 0
                    }
                    delta < 0 -> {
                        // The states from the current issuer don't cover this payment, so we'll have to use >1 output
                        // state to cover this payment.
                        outputStates += deriveState(templateState, remainingFromCurrentIssuer, party)
                        remainingFromEachIssuer.removeAt(remainingFromEachIssuer.lastIndex)
                        remainingToPay -= remainingFromCurrentIssuer.quantity
                    }
                }
            }
        }

        // Whatever values we have left over for each issuer must become change outputs.
        for ((token, amount) in remainingFromEachIssuer) {
            val templateState = statesGroupedByIssuer[token]!!.first().state
            outputStates += deriveState(templateState, amount, payChangeTo)
        }

        for (state in gathered) tx.addInputState(state)
        for (state in outputStates) tx.addOutputState(state)

        // What if we already have a move command with the right keys? Filter it out here or in platform code?
        tx.addCommand(generateMoveCommand(), keysUsed)

        return Triple(tx, keysUsed, gathered.map { it.state.data } + outputStates.map { it.data })
    }


    /**
     * Gather assets from the given list of states, sufficient to match or exceed the given amount.
     *
     * @param acceptableCoins list of states to use as inputs.
     * @param amount the amount to gather states up to.
     * @throws InsufficientBalanceException if there isn't enough value in the states to cover the requested amount.
     */
    @Throws(InsufficientBalanceException::class)
    private fun <S : FungibleAsset<T>, T : Any> gatherCoins(acceptableCoins: Collection<StateAndRef<S>>,
                                                            amount: Amount<T>): Pair<ArrayList<StateAndRef<S>>, Amount<T>> {
        require(amount.quantity > 0) { "Cannot gather zero coins" }
        val gathered = arrayListOf<StateAndRef<S>>()
        var gatheredAmount = Amount(0, amount.token)
        for (c in acceptableCoins) {
            if (gatheredAmount >= amount) break
            gathered.add(c)
            gatheredAmount += Amount(c.state.data.amount.quantity, amount.token)
        }

        if (gatheredAmount < amount) {
            throw InsufficientBalanceException(amount - gatheredAmount)
        }

        return Pair(gathered, gatheredAmount)
    }
}

/**
 * Responder flow for [OfferEncumberedTokensFlow].
 * Finalise push token transaction.
 */
@InitiatedBy(OfferEncumberedTokensFlow::class)
class OfferEncumberedTokensFlowHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call()  {
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

@CordaService
class CacheService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    init {
        //serviceHub.register { processEvent(it) }
    }

//    private fun processEvent(event: ServiceLifecycleEvent) {
//        when (event) {
//            ServiceLifecycleEvent.STATE_MACHINE_STARTED -> {
//            } else -> {
//            }
//        }
//    }

    companion object {
        private val transactions = mutableMapOf<SecureHash, WireTransaction>()
    }

    fun getWireTransactionById(txId: SecureHash): WireTransaction? {
        return transactions[txId]
    }

    fun getWireTransactionById(txId: SecureHash, party: Party): WireTransaction? {
        return transactions[txId]
    }

    fun cacheWireTransaction(tx: WireTransaction, party: Party): Unit {
        transactions[tx.id] = tx
    }
}

fun ServiceHub.cacheService(): CacheService = this.cordaService(CacheService::class.java)