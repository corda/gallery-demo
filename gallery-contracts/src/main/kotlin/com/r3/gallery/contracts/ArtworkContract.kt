package com.r3.gallery.contracts

import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class ArtworkContract : Contract {
    companion object {
        const val ARTWORK_CONTRACT_ID = "com.r3.gallery.contracts.ArtworkContract"
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
            }

            class List : TypeOnlyCommandData(), Commands {
                override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                }
            }

            class Delist : TypeOnlyCommandData(), Commands {
                override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                }
            }
        }
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        command.value.verifyCommand(tx, command.signers.toSet())
    }
}