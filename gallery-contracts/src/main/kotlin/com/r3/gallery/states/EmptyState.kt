package com.r3.gallery.states

import com.r3.gallery.contracts.EmptyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(EmptyContract::class)
data class EmptyState(override val participants: List<AbstractParty>) : ContractState