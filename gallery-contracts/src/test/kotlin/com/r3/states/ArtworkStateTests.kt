package com.r3.states

import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.LinearState
import org.junit.Test

class ArtworkStateTests {

    @Test
    fun `is linear state`() {
        assert(LinearState::class.java.isAssignableFrom(ArtworkState::class.java))
    }
}