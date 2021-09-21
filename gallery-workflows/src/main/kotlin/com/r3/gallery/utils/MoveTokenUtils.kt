package com.r3.gallery.utils

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.utilities.addTokenTypeJar
import com.r3.gallery.contracts.LockContract
import com.r3.gallery.states.LockState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.CompositeKey
import net.corda.core.identity.AbstractParty
import net.corda.core.internal.requiredContractClassName
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey

/**
 * Adds token move to transaction. [amount] and [holder] parameters specify which party should receive the amount of
 * token, with possible change paid to [changeHolder].
 * Note: For now this method always uses database token selection, to use in memory one, use [addMoveTokens] with
 * already selected input and output states. []
 */
@Suspendable
internal fun TransactionBuilder.addMoveTokens(
    serviceHub: ServiceHub,
    amount: Amount<TokenType>,
    holder: AbstractParty,
    changeHolder: AbstractParty,
    additionalKeys: List<PublicKey>,
    lockState: LockState? = null
): TransactionBuilder {
    val selector = DatabaseTokenSelection(serviceHub)
    val (inputs, outputs) = selector.generateMove(
        listOf(Pair(holder, amount)),
        changeHolder,
        TokenQueryBy(),
        this.lockId
    )
    return this.addMoveTokens(
        inputs = inputs,
        outputs = outputs,
        additionalKeys,
        lockState
    )
}

/**
 * Adds a set of token moves to a transaction using specific inputs and outputs. If a [lockState] is passed in, it will
 * be used as an encumbrance for any token that is being moved to a new holder where the holder is a composite key.
 */
@Suspendable
internal fun TransactionBuilder.addMoveTokens(
    inputs: List<StateAndRef<AbstractToken>>,
    outputs: List<AbstractToken>,
    additionalKeys: List<PublicKey>,
    lockState: LockState? = null
): TransactionBuilder {
    val outputGroups: Map<IssuedTokenType, List<AbstractToken>> = outputs.groupBy { it.issuedTokenType }
    val inputGroups: Map<IssuedTokenType, List<StateAndRef<AbstractToken>>> = inputs.groupBy {
        it.state.data.issuedTokenType
    }

    check(outputGroups.keys == inputGroups.keys) {
        "Input and output token types must correspond to each other when moving tokensToIssue"
    }

    var previousEncumbrance = outputs.size

    this.apply {
        // Add a notary to the transaction.
        notary = inputs.map { it.state.notary }.toSet().single()
        outputGroups.forEach { (issuedTokenType: IssuedTokenType, outputStates: List<AbstractToken>) ->
            val inputGroup = inputGroups[issuedTokenType]
                ?: throw IllegalArgumentException("No corresponding inputs for the outputs issued token type: $issuedTokenType")
            val keys = inputGroup.map { it.state.data.holder.owningKey }.distinct()

            var inputStartingIdx = inputStates().size
            var outputStartingIdx = outputStates().size

            val inputIdx = inputGroup.map {
                addInputState(it)
                inputStartingIdx++
            }

            val outputIdx = outputStates.map {
                if (lockState != null && it.holder.owningKey is CompositeKey) {
                    addOutputState(it, it.requiredContractClassName!!, notary!!, previousEncumbrance)
                    previousEncumbrance = outputStartingIdx
                } else {
                    addOutputState(it)
                }
                outputStartingIdx++
            }

            addCommand(
                MoveTokenCommand(issuedTokenType, inputs = inputIdx, outputs = outputIdx),
                keys + additionalKeys
            )
        }

        if (lockState != null) {
            addOutputState(
                state = lockState,
                contract = LockContract.contractId,
                notary = notary!!,
                encumbrance = previousEncumbrance
            )
            addCommand(LockContract.Encumber(), lockState.getCompositeKey())
        }
    }

    addTokenTypeJar(inputs.map { it.state.data } + outputs, this)

    return this
}
