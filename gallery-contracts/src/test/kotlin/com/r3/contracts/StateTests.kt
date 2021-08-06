package com.r3.contracts

import com.r3.gallery.states.EmptyState
import net.corda.core.contracts.ContractState
import org.junit.Test

class StateTests {
    @Test
    fun `dummy test`() {
        assert(EmptyState(emptyList()).participants.isEmpty())
    }
}