package com.r3.contracts

import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.LinearState
import org.junit.Test

class ArtworkContractTests {

    @Test
    fun `is linear state`() {
        assert(LinearState::class.java.isAssignableFrom(ArtworkState::class.java))
    }
}