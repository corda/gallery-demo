package com.r3.gallery.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class EmptyContract : Contract {
    companion object {
        const val ID = "com.r3.gallery.contracts.EmptyContract"
    }
    override fun verify(tx: LedgerTransaction) {
        // empty
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}