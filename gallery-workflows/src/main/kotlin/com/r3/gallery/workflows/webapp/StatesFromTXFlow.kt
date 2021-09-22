package com.r3.gallery.workflows.webapp

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.NamedByHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction

@StartableByRPC
class StatesFromTXFlow(private val namedByHash: NamedByHash): FlowLogic<List<ContractState>>() {

    @Suspendable
    override fun call(): List<ContractState> {
        val tx: LedgerTransaction = when (namedByHash) {
            is SignedTransaction -> { namedByHash.toLedgerTransaction(serviceHub) }
            is WireTransaction -> { namedByHash.toLedgerTransaction(serviceHub) }
            else -> throw Exception("Unable to create Ledger Transaction from $namedByHash")
        }
        return tx.inputStates + tx.outputStates + tx.referenceStates
    }
}