package com.r3.gallery.workflows

import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.WireTransaction

@CordaService
class CacheService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    private companion object {
        val transactions = mutableMapOf<SecureHash, WireTransaction>()
    }

    fun getWireTransactionById(txId: SecureHash): WireTransaction? {
        return transactions[txId]
    }

    fun getWireTransactionById(txId: SecureHash, party: Party): WireTransaction? {
        return transactions[txId]
    }

    fun cacheWireTransaction(tx: WireTransaction, party: Party): Unit {
        transactions[tx.id] = tx
    }
}

fun ServiceHub.cacheService(): CacheService = this.cordaService(CacheService::class.java)