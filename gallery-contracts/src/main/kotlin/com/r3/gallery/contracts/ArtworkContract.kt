package com.r3.gallery.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class ArtworkContract: Contract {
    companion object {
        const val ID:String = "com.r3.gallery.contracts.ArtworkContract"
    }

    override fun verify(tx: LedgerTransaction) {
    }

    interface Commands : CommandData {
        class Issue : Commands
        class Transfer : Commands
    }
}