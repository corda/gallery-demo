package com.r3.gallery.contracts

import com.r3.gallery.states.EmptyState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
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