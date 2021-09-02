package com.r3.payments.currency

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.corda.core.contracts.Amount
import net.corda.core.contracts.TokenizableAssetInfo
import net.corda.core.serialization.CordaSerializable
import java.math.BigDecimal
import java.util.*

@CordaSerializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class FiatCurrency(val currency: Currency) : TokenizableAssetInfo {
    override val displayTokenSize: BigDecimal
        get() = BigDecimal.ONE.scaleByPowerOfTen(-currency.defaultFractionDigits)
}

val GBP = FiatCurrency(Currency.getInstance("GBP"))
val USD = FiatCurrency(Currency.getInstance("USD"))
val EUR = FiatCurrency(Currency.getInstance("EUR"))
val SGD = FiatCurrency(Currency.getInstance("SGD"))
val AUD = FiatCurrency(Currency.getInstance("AUD"))
val JPY = FiatCurrency(Currency.getInstance("JPY"))

fun fiatCurrencies(): Set<FiatCurrency> = setOf(GBP, USD, EUR, SGD, AUD, JPY)

fun Amount<Currency>.toFiatCurrency() = Amount(this.quantity, FiatCurrency(this.token))

// -------------------------------------------------------------------------------
// Helpers for creating an amount of a currency using some quantity and a fiat currency.
// -------------------------------------------------------------------------------

/** For creating [Int] quantities of [FiatCurrency]s. */
fun amount(amount: Int, currency: FiatCurrency): Amount<FiatCurrency> = amount(amount.toLong(), currency)

/** For creating [Long] quantities of [FiatCurrency]s. */
fun amount(amount: Long, currency: FiatCurrency): Amount<FiatCurrency> = Amount.fromDecimal(BigDecimal.valueOf(amount), currency)

/** For creating [Double] quantities of [FiatCurrency]s.  */
fun amount(amount: Double, currency: FiatCurrency): Amount<FiatCurrency> = Amount.fromDecimal(BigDecimal.valueOf(amount), currency)

/** For creating [BigDecimal] quantities of [FiatCurrency]s. */
fun amount(amount: BigDecimal, currency: FiatCurrency): Amount<FiatCurrency> = Amount.fromDecimal(amount, currency)

// ---------------------------------------------------------------------------------------------
// For creating amounts of token types using a DSL-like infix notation. E.g. "1000 of GBP"
// ---------------------------------------------------------------------------------------------

/** For creating [Int] quantities of [FiatCurrency]s. */
infix fun Int.of(currency: FiatCurrency): Amount<FiatCurrency> = amount(this, currency)

/** For creating [Long] quantities of [FiatCurrency]s. */
infix fun Long.of(currency: FiatCurrency): Amount<FiatCurrency> = amount(this, currency)

/** For creating [Double] quantities of [FiatCurrency]s. */
infix fun Double.of(currency: FiatCurrency): Amount<FiatCurrency> = amount(this, currency)

/** For creating [BigDecimal] quantities of [FiatCurrency]s. */
infix fun BigDecimal.of(currency: FiatCurrency): Amount<FiatCurrency> = amount(this, currency)