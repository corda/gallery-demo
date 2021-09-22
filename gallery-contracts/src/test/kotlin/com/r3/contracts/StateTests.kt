package com.r3.contracts

import com.r3.gallery.states.EmptyState
import org.junit.Test

class StateTests {
    @Test
    fun `dummy test`() {
        assert(EmptyState(emptyList()).participants.isEmpty())
    }
}