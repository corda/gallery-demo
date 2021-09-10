package com.r3.gallery.contracts

import com.r3.gallery.states.LockState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.crypto.TransactionSignature
import net.corda.core.crypto.toStringShort
import net.corda.core.transactions.LedgerTransaction

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

				require(ourInputs.isEmpty()){
					"To create an Encumbered Lock, there must be no input Lock states"
				}

				require(ourOutputs.size == 1){
					"To create an Encumbered Lock, there must be a single output Lock state"
				}

				require(tx.outputs.size > 1) {
					"No non-lock states found to encumber"
				}

				val lockState = ourOutputs.single()
				require(lockState.participants.toSet().size == 2){
					"The lock state must have 2 different participants"
				}

				val signer = ourCommand.signers.single()

				require(signer == lockState.getCompositeKey() ){
					"The lockstate must be signed by a composite key derived from an equal weighting of the two participants"
				}

				require(lockState.timeWindow.untilTime != null ) {
					"The time window on the lockstate must have an untilTime specified"
				}

				val timeWindowUntil = tx.timeWindow?.untilTime
				require(timeWindowUntil != null &&
						timeWindowUntil >= lockState.timeWindow.untilTime ) {
					"The time window on the lockstate must have a greater untilTime than the lockState"
				}

				require(tx.outputs.filter { it.contract == contractId }.single().encumbrance != null) {
					"The lock state must be encumbered"
				}

				// TODO - the encumbrance logic in Corda should check for cyclic encumbrance dependency.
				//  need to check whether there are any other conditions we should check for
			}
			is Release -> {
				val signature = (ourCommand.value as Release).signature
				val ourState = tx.inRefsOfType(LockState::class.java).single().state.data
				require(signature.isValid(ourState.txHash.txId)) {
					"Signature provided is not valid for encumberance transaction"
				}
				require(signature.signatureMetadata == ourState.txHash.signatureMetadata) {
					"Signature scheme information is not consistent with lock setup"
				}
				require(signature.by.toStringShort() == ourState.controllingNotary.owningKey.toStringShort()) {
					"Signer of encumberance transaction does not match controlling notary in Lock setup"
				}
			}
        }
    }

    open class LockCommand : CommandData
    class Encumber() : LockCommand()
    class Release(val signature: TransactionSignature) : LockCommand()
}

