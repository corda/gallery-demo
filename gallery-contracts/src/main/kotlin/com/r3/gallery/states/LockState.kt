package com.r3.gallery.states

import com.r3.gallery.contracts.LockContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.SignableData
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.security.PublicKey

@BelongsToContract(LockContract::class)
data class LockState(
    val txHash: SignableData,
    val creator: Party,
    val receiver: Party,
    val controllingNotary: Party,
    val timeWindow: TimeWindow,
    override val participants: List<AbstractParty> = listOf(creator, receiver)
) : ContractState {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LockState
        if (txHash != other.txHash) return false
        if (creator != other.creator) return false
        if (receiver != other.receiver) return false
        if (controllingNotary != other.controllingNotary) return false
        if (timeWindow != other.timeWindow) return false
        return true
    }

    fun getCompositeKey(): PublicKey {
        return CompositeKey.Builder()
            .addKeys(participants.map { it.owningKey })
            .build(1)
    }
}
