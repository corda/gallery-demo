package com.r3.gallery.contracts

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.gallery.states.LockState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.TransactionSignature
import net.corda.core.crypto.toStringShort
import net.corda.core.transactions.LedgerTransaction
import java.time.Instant

class LockContract : Contract {

    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName!!
    }

    override fun verify(tx: LedgerTransaction) {

        val ourCommand = tx.commandsOfType(LockCommand::class.java).single()
        val ourInputs = tx.inputsOfType(LockState::class.java)
        val ourOutputs = tx.outputsOfType(LockState::class.java)

        when (ourCommand.value) {
            is Encumber -> {

                require(ourInputs.isEmpty()) {
                    "To create an Encumbered Lock, there must be no input Lock states"
                }

                require(ourOutputs.size == 1) {
                    "To create an Encumbered Lock, there must be a single output Lock state"
                }

                require(tx.outputs.size > 1) {
                    "No non-lock states found to encumber"
                }

                val lockState = ourOutputs.single()
                require(lockState.participants.toSet().size == 2) {
                    "The lock state must have 2 different participants"
                }

                val signer = ourCommand.signers.single()

                require(signer == lockState.getCompositeKey()) {
                    "The lock state must be signed by a composite key derived from an equal weighting of the two participants"
                }

                require(lockState.timeWindow.untilTime != null) {
                    "The time window on the lock state must have an untilTime specified"
                }

                val timeWindowUntil = tx.timeWindow?.untilTime
                require(
                    timeWindowUntil != null &&
                            timeWindowUntil >= lockState.timeWindow.untilTime
                ) {
                    "The time window on the lock state must have a greater untilTime than the lockState"
                }

                require(tx.outputs.single { it.contract == contractId }.encumbrance != null) {
                    "The lock state must be encumbered"
                }

                // TODO - the encumbrance logic in Corda should check for cyclic encumbrance dependency.
                //  need to check whether there are any other conditions we should check for
            }
            is Release -> {
                val signature = (ourCommand.value as Release).signature
                val ourState = tx.inRefsOfType(LockState::class.java).single().state.data
                require(signature.isValid(ourState.txHash.txId)) {
                    "Signature provided is not valid for encumbrance transaction"
                }
                require(signature.signatureMetadata == ourState.txHash.signatureMetadata) {
                    "Signature scheme information is not consistent with lock setup"
                }
                require(signature.by.toStringShort() == ourState.controllingNotary.owningKey.toStringShort()) {
                    "Signer of encumbrance transaction does not match controlling notary in Lock setup"
                }
            }
            is Redeem -> {
                val now = Instant.now()
                val ourState = tx.inRefsOfType(LockState::class.java).single().state.data
                val encumberedTxIssuer = ourState.creator
                val encumberedTxUntilTime = ourState.timeWindow.untilTime!!

                val allowedOutputs: Set<FungibleToken> =
                    tx.inputsOfType(FungibleToken::class.java).map { it.withNewHolder(encumberedTxIssuer) }.toSet()
                val actualOutputs: Set<FungibleToken> = tx.outputsOfType(FungibleToken::class.java).toSet()

                requireThat {
                    "Tokens can only be redeemed after the lock state expired" using (now.isAfter(encumberedTxUntilTime))
                    "Tokens can only be redeemed by the issuer of the encumbered offer" using (allowedOutputs == actualOutputs)
                }
            }
        }
    }

    open class LockCommand : CommandData
    class Encumber : LockCommand()
    class Release(val signature: TransactionSignature) : LockCommand()
    class Redeem : LockCommand()
}

