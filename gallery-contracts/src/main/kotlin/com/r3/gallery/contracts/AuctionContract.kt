package com.r3.gallery.contracts

import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class AuctionContract : Contract {
    override fun verify(tx: LedgerTransaction) {
    }
}