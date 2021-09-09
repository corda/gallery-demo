package com.r3.gallery.contracts

import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.*
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class ArtworkContract : Contract {
    companion object {
        const val ID = "com.r3.gallery.contracts.ArtworkContract"
    }

    interface Commands : CommandData {
        fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>)

        class Issue : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                requireThat {
                    "Unexpected inputs when issuing an artwork item" using tx.inputStates.isEmpty()
                    "Issuing an artwork item requires a single output state" using (tx.outputStates.size == 1)
                    "The output state must be of type ${ArtworkState::class.java.name}" using (tx.outputStates.single() is ArtworkState)
                    val outputState = tx.outputStates.single() as ArtworkState
                    "Only the owner needs to sign the transaction" using (signers == setOf(outputState.owner.owningKey))
                }
            }
        }

        class TransferOwnership : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                requireThat {
                    "Expecting only one AuctionItemState input" using (tx.inputsOfType(ArtworkState::class.java).size == 1)
                    "Expected only one AuctionItemState output" using (tx.outputsOfType(ArtworkState::class.java).size == 1)
                    val inputItem = tx.inputsOfType(ArtworkState::class.java).single()
                    val outputItem = tx.outputsOfType(ArtworkState::class.java).single()
                    "Only the 'owner' property can change" using (inputItem == outputItem.copy(owner = inputItem.owner))
                    "The 'owner' property must change" using (outputItem.owner != inputItem.owner)
                    "The previous and new owner only must sign a transfer transaction" using (signers == setOf(outputItem.owner.owningKey, inputItem.owner.owningKey))
                }
            }
        }
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        command.value.verifyCommand(tx, command.signers.toSet())
    }
}