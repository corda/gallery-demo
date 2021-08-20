package com.r3.gallery.contracts

import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class ArtworkContract: Contract {
    override fun verify(tx: LedgerTransaction) {
    }
}