package com.r3.gallery.states

import com.r3.gallery.contracts.LockContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.SignableData
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

@CordaSerializable
data class LockStateBase(
    val txHash: SignableData,
    val controllingNotary: Party,
    val timeWindow: TimeWindow,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LockStateBase
        if (txHash != other.txHash) return false
        if (controllingNotary != other.controllingNotary) return false
        if (timeWindow != other.timeWindow) return false
        return true
    }

    override fun hashCode(): Int {
        var result = txHash.hashCode()
        result = 31 * result + controllingNotary.hashCode()
        result = 31 * result + timeWindow.hashCode()
        return result
    }
}

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

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + txHash.hashCode()
        result = 31 * result + creator.hashCode()
        result = 31 * result + receiver.hashCode()
        result = 31 * result + controllingNotary.hashCode()
        result = 31 * result + timeWindow.hashCode()
        result = 31 * result + participants.hashCode()
        return result
    }
}

