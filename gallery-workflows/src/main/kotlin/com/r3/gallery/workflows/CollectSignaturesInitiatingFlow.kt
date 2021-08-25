import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
class CollectSignaturesInitiatingFlow(
        private val transaction: SignedTransaction,
        private val signers: List<Party>
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // create new sessions to signers and trigger the signing responder flow
        val sessions = signers.map { initiateFlow(it) }
        return subFlow(CollectSignaturesFlow(transaction, sessions))
    }
}

@InitiatedBy(CollectSignaturesInitiatingFlow::class)
class CollectSignaturesResponder(private val session: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // sign the transaction and nothing else
        return subFlow(object : SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) {
                println("Signing TX : ${ourIdentity.name}")
            }
        })
    }
}