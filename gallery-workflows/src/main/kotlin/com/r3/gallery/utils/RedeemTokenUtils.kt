package com.r3.gallery.utils

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.sumTokenStateAndRefs
import com.r3.corda.lib.tokens.workflows.utilities.addTokenTypeJar
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.TransactionBuilder

/**
 * Add redeeming of multiple [inputs] to the [transactionBuilder] with possible [changeOutput].
 */
@Suspendable
@JvmOverloads
fun TransactionBuilder.addTokensToRedeem(
    inputs: List<StateAndRef<AbstractToken>>,
    changeOutput: AbstractToken? = null
): TransactionBuilder {

    val firstState = inputs.first().state
    val firstNotary = firstState.notary

    val issuerToCheck = changeOutput?.issuer ?: firstState.data.issuer
    check(inputs.all { it.state.data.issuer == issuerToCheck }) {
        "Tokens with different issuers."
    }

    check(inputs.all { it.state.notary == firstNotary }) {
        "All states should have the same notary. Automatic notary change isn't supported for now."
    }

    if (this.notary == null) {
        this.notary = firstNotary
    } else {
        check(this.notary == firstNotary) {
            "Notary passed to transaction builder (${this.notary}) should be the same as the one used by input states ($firstNotary)."
        }
    }

    if (changeOutput != null && changeOutput is FungibleToken) {
        check(inputs.filterIsInstance<StateAndRef<FungibleToken>>().sumTokenStateAndRefs() > changeOutput.amount) {
            "Change output should be less than sum of inputs."
        }
    }

    val issuerKey = firstState.data.issuer.owningKey
    val moveKeys = inputs.map { it.state.data.holder.owningKey }

    var inputIdx = this.inputStates().size
    val outputIdx = this.outputStates().size
    this.apply {
        val inputIndices = inputs.map {
            addInputState(it)
            inputIdx++
        }
        val outputs = if (changeOutput != null) {
            addOutputState(changeOutput)
            listOf(outputIdx)
        } else {
            emptyList()
        }
        addCommand(RedeemTokenCommand(firstState.data.issuedTokenType, inputIndices, outputs), moveKeys + issuerKey)
    }
    val states = inputs.map { it.state.data } + if (changeOutput == null) emptyList() else listOf(changeOutput)
    addTokenTypeJar(states, this)
    return this
}
