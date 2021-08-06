package com.r3.contracts

import com.r3.gallery.contracts.EmptyContract
import com.r3.gallery.states.EmptyState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val ledgerServices: MockServices = MockServices(listOf("com.r3.gallery.contracts"))
    var alice = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))
    var bob = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))

    @Test
    fun `dummy test`() {
        val state = EmptyState(listOf(alice.party, bob.party))
        ledgerServices.ledger {
            //pass
            transaction {
                //passing transaction
                output(EmptyContract.ID, state)
                command(alice.publicKey, EmptyContract.Commands.Create())
                verifies()
            }
        }
    }
}