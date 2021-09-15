package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.broker.corda.rpc.service.ConnectionService

internal fun ConnectionManager.connectToCurrencyNetwork(currency: String) : ConnectionService {
    require(currency.equals("GBP", true) || currency.equals("CBDC", true))
    return if (currency.equals("GBP", ignoreCase = true)) this.gbp
    else this.cbdc
}